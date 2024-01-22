package com.plcoding.stockmarketapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompanyListing(
        companyListingEntity: List<CompanyListingEntity>
    )

    @Query("DELETE FROM company_listing_entity")
    suspend fun clearCompanyListing()

    //ví dụ: search tEs -> %tes% or %TES%
    @Query(
        """
            SELECT * FROM company_listing_entity 
            WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR UPPER(:query) == symbol
        """
    )
    suspend fun searchCompanyListing(query: String): List<CompanyListingEntity>
}