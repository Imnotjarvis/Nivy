package com.ivy.onboarding

import com.ivy.base.model.AccountType
import androidx.compose.ui.graphics.toArgb
import arrow.core.raise.either
import com.ivy.base.resource.ResourceProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.IconAsset
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CategoryRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.ui.R
import com.ivy.wallet.domain.logic.model.CreateAccountData
import com.ivy.wallet.domain.logic.model.CreateCategoryData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID
import javax.inject.Inject
import com.ivy.design.system.colors.Palette

/** The accounts and categories a new wallet can start with. */
class StarterData @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val currencyRepository: CurrencyRepository,
    private val resourceProvider: ResourceProvider,
) {
    suspend fun preloadAccounts() {
        val baseCurrency = currencyRepository.getBaseCurrency()
        listOf(
            Triple(R.string.cash, Palette.Green, "cash"),
            Triple(R.string.bank, Palette.IvyDark, "bank"),
        ).forEachIndexed { index, (name, color, icon) ->
            either {
                Account(
                    id = AccountId(UUID.randomUUID()),
                    name = NotBlankTrimmedString.from(resourceProvider.getString(name)).bind(),
                    asset = baseCurrency,
                    color = ColorInt(color.toArgb()),
                    icon = IconAsset.from(icon).getOrNull(),
                    includeInBalance = true,
                    type = AccountType.OTHER,
                    archived = false,
                    creditLimit = null,
                    chargeDay = null,
                    statementClosingDay = null,
                    orderNum = index.toDouble(),
                )
            }.getOrNull()?.let { accountRepository.save(it) }
        }
    }

    fun accountSuggestions(baseCurrency: String): ImmutableList<CreateAccountData> =
        persistentListOf(
            CreateAccountData(
                name = resourceProvider.getString(R.string.cash),
                currency = baseCurrency,
                color = Palette.Green,
                icon = "cash",
                balance = 0.0
            ),
            CreateAccountData(
                name = resourceProvider.getString(R.string.bank),
                currency = baseCurrency,
                color = Palette.IvyDark,
                icon = "bank",
                balance = 0.0
            ),
            CreateAccountData(
                name = resourceProvider.getString(R.string.revoult),
                currency = baseCurrency,
                color = Palette.Blue,
                icon = "revolut",
                balance = 0.0
            ),
        )

    suspend fun preloadCategories() {
        preloadCategoriesCreateData().forEachIndexed { index, createData ->
            preloadCategory(createData, orderNum = index.toDouble())
        }
    }

    private fun preloadCategoriesCreateData() = listOf(
        CreateCategoryData(
            name = resourceProvider.getString(R.string.food_drinks),
            color = Palette.Green,
            icon = "fooddrink"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.bills_fees),
            color = Palette.Red,
            icon = "bills"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.transport),
            color = Palette.YellowLight,
            icon = "transport"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.groceries),
            color = Palette.GreenLight,
            icon = "groceries"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.entertainment),
            color = Palette.Orange,
            icon = "game"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.shopping),
            color = Palette.Ivy,
            icon = "shopping"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.gifts),
            color = Palette.RedLight,
            icon = "gift"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.health),
            color = Palette.IvyLight,
            icon = "health"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.investments),
            color = Palette.IvyDark,
            icon = "leaf"
        ),

        CreateCategoryData(
            name = resourceProvider.getString(R.string.loans),
            color = Palette.BlueDark,
            icon = "loan"
        ),
    )

    private suspend fun preloadCategory(
        data: CreateCategoryData,
        orderNum: Double,
    ) {
        val category: Category? = either {
            Category(
                name = NotBlankTrimmedString.from(data.name).bind(),
                color = ColorInt(data.color.toArgb()),
                icon = data.icon?.let(IconAsset::from)?.getOrNull(),
                orderNum = orderNum,
                id = CategoryId(UUID.randomUUID()),
            )
        }.getOrNull()

        if (category != null) {
            categoryRepository.save(category)
        }
    }

    fun categorySuggestions(): ImmutableList<CreateCategoryData> = preloadCategoriesCreateData()
        .plus(
            listOf(
                CreateCategoryData(
                    name = resourceProvider.getString(R.string.car),
                    color = Palette.Blue3,
                    icon = "vehicle"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.work),
                    color = Palette.Blue2Light,
                    icon = "work"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.home_category),
                    color = Palette.Green2,
                    icon = "house"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.restaurant),
                    color = Palette.Orange3,
                    icon = "restaurant"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.family),
                    color = Palette.Red3Light,
                    icon = "family"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.social_life),
                    color = Palette.Blue2,
                    icon = "people"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.order_food),
                    color = Palette.Orange2,
                    icon = "orderfood2"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.travel),
                    color = Palette.BlueLight,
                    icon = "travel"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.fitness),
                    color = Palette.Purple2,
                    icon = "fitness"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.self_development),
                    color = Palette.Yellow,
                    icon = "selfdevelopment"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.clothes),
                    color = Palette.Green2Light,
                    icon = "clothes2"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.beauty),
                    color = Palette.Red3,
                    icon = "makeup"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.education),
                    color = Palette.Blue,
                    icon = "education"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.pet),
                    color = Palette.Orange3Light,
                    icon = "pet"
                ),

                CreateCategoryData(
                    name = resourceProvider.getString(R.string.sports),
                    color = Palette.Purple1,
                    icon = "sports"
                ),
            )
        ).toImmutableList()
}
