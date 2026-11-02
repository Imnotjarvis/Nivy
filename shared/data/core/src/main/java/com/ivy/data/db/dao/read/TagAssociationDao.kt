package com.ivy.data.db.dao.read

import androidx.room.Dao
import androidx.room.MapColumn
import androidx.room.Query
import com.ivy.data.db.entity.TagAssociationEntity
import java.util.UUID

@Dao
interface TagAssociationDao {
    @Query("SELECT * FROM tags_association")
    suspend fun findAll(): List<TagAssociationEntity>

    @Suppress(
        "ArgumentListWrapping",
        "ParameterListWrapping",
        "AnnotationOnSeparateLine",
        "MaximumLineLength",
        "MaxLineLength"
    )
    @Query("SELECT * FROM tags_association WHERE tagId in (:tagIds)")
    suspend fun findByAllAssociatedIdForTagId(tagIds: List<UUID>): Map<@MapColumn(columnName = "tagId") UUID, List<TagAssociationEntity>>
}
