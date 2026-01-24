package uqu.drawbridge.platform.repository

import org.springframework.data.jpa.repository.JpaRepository
import uqu.drawbridge.platform.model.Category

interface CategoryRepository : JpaRepository<Category, String> {
    fun findByName(name: String): Category?
    fun findByParentCategoryId(parentCategoryId: String?): List<Category>
}
