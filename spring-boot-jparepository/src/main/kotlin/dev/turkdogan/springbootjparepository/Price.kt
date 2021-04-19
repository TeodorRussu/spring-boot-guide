package dev.turkdogan.springbootjparepository

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.persistence.*

@Entity
class Price {

    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(nullable = false)
    var value: BigDecimal = BigDecimal.ZERO

    @Column(nullable = false)
    var date: Instant? = null
}