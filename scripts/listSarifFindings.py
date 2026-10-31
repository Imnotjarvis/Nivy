"""Prints a SARIF file's findings the way the Security tab ranks them, so a CodeQL job's log
shows them too.
Usage: listSarifFindings.py results.sarif [more.sarif ...]
"""
import json
import sys


def severity(rule):
    props = rule.get("properties", {})
    score = props.get("security-severity")
    if score is not None:
        s = float(score)
        return "critical" if s >= 9 else "high" if s >= 7 else "medium" if s >= 4 else "low"
    return props.get("problem.severity") or rule.get("defaultConfiguration", {}).get("level", "?")


ORDER = ["critical", "high", "medium", "low", "error", "warning", "recommendation", "note"]

for path in sys.argv[1:]:
    for run in json.load(open(path)).get("runs", []):
        tool = run["tool"]
        rules = {r["id"]: r for c in [tool["driver"], *tool.get("extensions", [])]
                 for r in c.get("rules", [])}
        found = []
        for res in run.get("results", []):
            rule_id = res.get("ruleId") or res.get("rule", {}).get("id")
            loc = (res.get("locations") or [{}])[0].get("physicalLocation", {})
            found.append((
                severity(rules.get(rule_id, {})), rule_id,
                f'{loc.get("artifactLocation", {}).get("uri", "?")}:{loc.get("region", {}).get("startLine", "?")}',
                res.get("message", {}).get("text", "").replace("\n", " "),
            ))
        found.sort(key=lambda f: ORDER.index(f[0]) if f[0] in ORDER else len(ORDER))
        print(f"{path}: {len(found)} finding(s)")
        for sev, rule_id, where, message in found:
            print(f"  [{sev}] {rule_id} {where}: {message}")
