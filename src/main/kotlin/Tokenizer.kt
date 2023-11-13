fun scanTokens(code: String): MutableList<Token> {
  return Tokenizer(code.toCharArray()).tokenizeCode()
}

val OPERATORS = operators.keys

data class Rule(
  var isSingle: Boolean,
  var name: String,
  var rule: (Char) -> Boolean,
)

val rules = mutableListOf(
  Rule(true, "OPERATOR") { OPERATORS.contains(it) },
  Rule(false, "NUMBER") { it.isDigit() || it == '.' },
  Rule(false, "ORDER") { it.isLetter() }
)

class Tokenizer(private val code: CharArray) {
  private var current = 0
  private var line = 1
  
  fun tokenizeCode(): MutableList<Token> {
    val tokens = mutableListOf<Token>()
    while (current < code.size) {
      var found = false
      var expr: String
      
      for (rule in rules) {
        if (rule.isSingle) {
          if (rule.rule.invoke(peek())) {
            found = tokens.add(Token(rule.name, "${advance()}", 1, line))
            break
          }
        } else {
          if (rule.rule.invoke(peek())) {
            expr = code[current++].toString()
            
            while (current < code.size && rule.rule.invoke(peek())) expr = "$expr${advance()}"
            found = tokens.add(Token(rule.name, expr, expr.length, line))
            break
          }
        }
      }
      
      if (!found) current++
    }
    return tokens.map { it.parse() }.toMutableList()
  }
  
  private fun peek(): Char = code[current]
  private fun advance(): Char = code[current++]
}
