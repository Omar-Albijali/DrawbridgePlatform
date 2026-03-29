package uqu.drawbridge.platform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class DrawbridgeApplication

fun main(args: Array<String>) {
    runApplication<DrawbridgeApplication>(*args)
}

