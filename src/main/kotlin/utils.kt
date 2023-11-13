val keywords = listOf(
  "sin",
  "cos",
  "tg",
  "ctg",
  "sqrt",
  "log",
  "ln",
  "pi",
  "e"
)
val operators = mapOf(
  '+' to "PLUS",
  '-' to "MINUS",
  '/' to "SLASH",
  '*' to "STAR",
  '^' to "HAT",
  '|' to "MOD",
  '(' to "LEFT_PAREN",
  ')' to "RIGHT_PAREN"
)

data class Token(var type: String, var value: String, val length: Int, val line: Int) {
  private var parsed: Boolean = false
  override fun toString(): String = "Token of type $type with value $value"
  fun parse(): Token {
    if (parsed) return this
    when (type) {
      "NUMBER" -> {
        val isDecimal = value.contains('.')
        value = if (isDecimal) value.toDouble().toString() else value.toInt().toString()
        type = if (isDecimal) "DEC" else "INT"
      }
      
      "ORDER" -> type = keywords.find { value.lowercase() == it }!!.uppercase()
      "OPERATOR" -> type = operators[value[0]]!!
      else -> throw Error("Something wrong ($value)")
    }
    return this.also { parsed = true }
  }
}