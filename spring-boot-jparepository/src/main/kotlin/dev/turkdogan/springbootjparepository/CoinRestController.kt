package dev.turkdogan.springbootjparepository

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
class CoinRestController {

    @Autowired
    lateinit var coinRepository: CoinRepository

    @GetMapping("/")
    fun getCoins(): List<Coin> {
        return coinRepository.findAll()
    }

    @GetMapping("/coinsSortedByNameDesc")
    fun getCoinsSortedByName(): List<Coin> {
        return coinRepository.findAllByOrderByNameDesc()
    }

    @GetMapping("/coinByName")
    fun getCoinByName(coinName: String): Coin {
        return coinRepository.findByName(coinName).get()
    }

    @GetMapping("/coinsByOrderByDescriptionDescNameAsc")
    fun getCoinsByMultipleSortParameters(): List<Coin> {
        return coinRepository.findAllByOrderByDescriptionDescNameAsc()
    }

    @GetMapping("/getFirstNCoins")
    fun getFirstCoins(count: Int): List<Coin> {
        return coinRepository.findAll(PageRequest.of(0, count)).content
    }

    @GetMapping("/getLastNCoins")
    fun getLastCoins(count: Int): List<Coin> {
        return coinRepository.findAllByOrderByStartDateDesc(PageRequest.of(0, count))
    }

    @DeleteMapping("/delete")
    fun deleteCoin(id: String) {
        println("id $id")
        coinRepository.deleteById(UUID.fromString(id))
    }
}