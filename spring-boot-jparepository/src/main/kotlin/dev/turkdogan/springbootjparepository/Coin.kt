package dev.turkdogan.springbootjparepository

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.Instant
import java.util.UUID
import javax.persistence.*

@Entity
class Coin {

    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(nullable = false, unique = true)
    var name: String = ""

    @Column
    var description: String = ""

    @CreatedDate
    var created = Instant.now()

    @LastModifiedDate
    var updated = Instant.now()

    @Column(nullable = false)
    var startDate: Instant? = null

    @OneToMany(cascade = [CascadeType.ALL])
    @JoinColumn(name = "coin_id")
    var priceList: MutableList<Price> = mutableListOf()
}