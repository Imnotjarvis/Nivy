#!/bin/bash
# TEMPORARY, for the editor split's on-device pass; removed before its pull request is merged. On
# the onboarded demo build the smoke test leaves (accounts Cash and Bank, the starter categories):
# a new expense through the amount, the category it then asks for and its title; the same expense
# opened again, with its description, tag, category and type dialogs, a tag added and chosen, and
# the type changed to a transfer to Bank; the new account dialog; the delete confirmation, closed
# with back; then a duplicate, and the original deleted. Screenshots go to build/smoke/editor/,
# with what UI Automator saw when a step didn't find its text.
set -u
cd "$(dirname "$0")/.." || exit 1

PACKAGE="${PACKAGE:-com.ivy.wallet.offline.debug}"
ACTIVITY="com.ivy.wallet.RootActivity"
OUT="build/smoke/editor"
mkdir -p "$OUT"
shots=0
failed=0

dump() {
    adb shell "rm -f /sdcard/ui.xml; uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; cat /sdcard/ui.xml 2>/dev/null"
}

# The bounds "x1 y1 x2 y2" of the $3rd element (default the first, '$' the last, which is the
# top dialog's) whose $1 attribute ("text" or "content-desc") is exactly $2, waiting up to 20
# seconds for it. When it doesn't come, what UI Automator saw is kept.
find_node() {
    local attribute=$1 value=$2 nth=${3:-1} start=$SECONDS screen node
    while (( SECONDS - start < 20 )); do
        screen=$(dump)
        node=$(grep -o "<node [^>]* $attribute=\"$value\"[^>]*>" <<< "$screen" | sed -n "${nth}p")
        if [ -n "$node" ]; then
            sed -E 's/.*bounds="\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\]".*/\1 \2 \3 \4/' <<< "$node"
            return 0
        fi
        sleep 1
    done
    echo "FAIL: no $attribute \"$value\" (#$nth) on screen" >&2
    printf '%s\n' "$screen" > "$OUT/missing-${value// /_}.xml"
    adb exec-out screencap -p > "$OUT/missing-${value// /_}.png"
    failed=1
    return 1
}

tap_node() {
    local x1 y1 x2 y2
    read -r x1 y1 x2 y2 < <(find_node "$@") || return 1
    adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
    echo "tapped $1 \"$2\" ${3:+#$3}"
    sleep 1
}

tap_text() { tap_node text "$@"; }
tap_desc() { tap_node content-desc "$@"; }

shot() {
    shots=$((shots + 1))
    sleep 1
    adb exec-out screencap -p > "$OUT/$(printf %02d $shots)-$1.png"
    echo "screenshot: $1"
}

# Typed into the focused field; spaces as adb's %s.
type_text() {
    adb shell input text "${1// /%s}"
    sleep 1
}

# The app from a cold start: Home, or the screen the given action opens.
launch() {
    adb shell cmd statusbar collapse
    adb shell am force-stop "$PACKAGE"
    adb shell am start -W -n "$PACKAGE/$ACTIVITY" "$@" > /dev/null
    sleep 3
}

# A new expense: the amount panel opens at once, then the category it asks for, then the title.
launch -a ivy.wallet.intent.action.add_transaction --es add_transaction_type_extra EXPENSE
shot new-amount
tap_text "4" && tap_text "2" && tap_text "Enter" && shot new-category-asked \
    && tap_text "Groceries" && shot new-title-focused \
    && type_text "Lunch" && adb shell input keyevent KEYCODE_ENTER && shot new-filled \
    && tap_text "Add" && sleep 2 && shot home-with-lunch

# The expense again, and its dialogs one by one.
launch
if tap_text "Lunch"; then
    shot edit-opened
    tap_text "Add description" && shot description-dialog \
        && type_text "Weekly shop" && tap_text "Add" && shot description-added
    tap_text "Add Tags" && shot tags-dialog \
        && tap_text "Add new" && shot tag-name-dialog \
        && type_text "work" && tap_text "Done" '$' && shot tag-added \
        && tap_text "#work" && shot tag-chosen \
        && tap_text "Done" && shot tags-closed
    tap_text "Groceries" && shot category-dialog \
        && tap_text "Transport" && shot category-changed
    tap_desc "Delete" && shot delete-dialog \
        && adb shell input keyevent KEYCODE_BACK && shot delete-cancelled
    tap_text "Add account" && shot new-account-dialog \
        && adb shell input keyevent KEYCODE_BACK && shot new-account-closed
    tap_text "Expense" && shot type-dialog \
        && tap_text "Transfer" && tap_text "Set" && shot transfer \
        && tap_text "Bank" 2 && shot transfer-to-bank \
        && tap_text "Save" && sleep 2 && shot home-with-transfer
fi

# A duplicate of it, then the original deleted.
launch
if tap_text "Lunch"; then
    shot transfer-opened
    tap_desc "Duplicate" && sleep 2 && shot home-with-duplicate
fi
launch
if tap_text "Lunch"; then
    tap_desc "Delete" && tap_text "Delete" && sleep 2 && shot home-after-delete
fi

echo "editor pass: $shots screenshots, failed=$failed"
exit 0
