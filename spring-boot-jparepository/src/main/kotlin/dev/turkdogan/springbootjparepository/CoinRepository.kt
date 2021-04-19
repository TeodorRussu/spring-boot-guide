package dev.turkdogan.springbootjparepository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CoinRepository : JpaRepository<Coin, UUID> {

    fun findAllByOrderByNameDesc(): List<Coin>

    fun findAllByOrderByStartDateDesc(page: Pageable): List<Coin>

    fun findAllByOrderByDescriptionDescNameAsc(): List<Coin>

    fun findByName(name: String): Optional<Coin>
}