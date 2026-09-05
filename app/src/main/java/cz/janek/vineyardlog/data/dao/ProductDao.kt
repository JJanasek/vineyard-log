package cz.janek.vineyardlog.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import cz.janek.vineyardlog.data.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY archived ASC, favorite DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun observe(id: Long): Flow<Product?>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun get(id: Long): Product?

    @Upsert
    suspend fun upsert(product: Product): Long

    @Insert
    suspend fun insertAll(products: List<Product>)

    @Query("SELECT COUNT(*) FROM product_usages WHERE productId = :productId")
    suspend fun usageCount(productId: Long): Int

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int
}
