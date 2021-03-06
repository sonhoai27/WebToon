package com.pluu.webtoon.domain.usecase

import com.pluu.webtoon.data.dao.IDBHelper
import com.pluu.webtoon.data.model.DBToon
import com.pluu.webtoon.model.NAV_ITEM
import javax.inject.Inject

/**
 * Delete Favorite Use Case
 */
class RemoveFavoriteUseCase @Inject constructor(
    private val dbHelper: IDBHelper
) {
    suspend operator fun invoke(type: NAV_ITEM, id: String) {
        dbHelper.removeFavorite(
            DBToon(
                service = type.name,
                toonId = id
            )
        )
    }
}
