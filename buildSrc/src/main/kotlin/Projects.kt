import java.nio.charset.Charset


fun String.exec(): String = Runtime.getRuntime().exec(this).inputStream.readBytes()
    .toString(Charset.defaultCharset()).trim()
