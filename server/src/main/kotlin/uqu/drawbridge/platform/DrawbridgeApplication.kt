package uqu.drawbridge.platform

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DrawbridgeApplication

fun main(args: Array<String>) {
    runApplication<DrawbridgeApplication>(*args)
}
