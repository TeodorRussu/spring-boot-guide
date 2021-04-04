package dev.turkdogan.springboothelloworld

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootHelloWorldApplication

fun main(args: Array<String>) {
	runApplication<SpringBootHelloWorldApplication>(*args)
}
