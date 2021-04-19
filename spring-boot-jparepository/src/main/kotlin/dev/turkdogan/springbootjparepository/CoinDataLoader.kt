package dev.turkdogan.springbootjparepository

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Component
class CoinDataLoader : ApplicationRunner {

    @Autowired
    private lateinit var coinRepository: CoinRepository

    override fun run(args: ApplicationArguments?) {
        for (i in 1..10) {
            val c = Coin()
            c.name = "coin $i"
            c.startDate = Instant.now().minus(i.toLong(), ChronoUnit.DAYS)
            c.description = "Description"

            for (d in 1..10) {
                val day = Instant.now().minus(d.toLong(), ChronoUnit.DAYS)
                val value = Random().nextDouble() * 100
                val price = Price()
                price.date = day
                price.value = BigDecimal.valueOf(value)
                c.priceList.add(price)
            }
            coinRepository.save(c)
        }
        coinRepository.flush()
    }
}