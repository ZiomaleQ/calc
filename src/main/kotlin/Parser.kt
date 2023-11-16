import java.math.BigDecimal
import java.math.MathContext

class Parser(private val code: MutableList<Token>) {
  private var lastToken: Token = Token("", "", 0, 0)
  
  fun parse(): MutableList<Node> {
    lastToken = getEOT()
    val output = mutableListOf<Node>()
    
    while (code.isNotEmpty()) {
      output.add(node())
    }
    
    return output
  }
  
  private fun node(): Node {
    var expr = rest()
    
    while (true) {
      expr = when {
        match("PLUS", "SLASH", "STAR", "MINUS", "HAT") -> {
          BinaryNode(
            op = lastToken.type, left = expr, right = node()
          )
        }
        
        else -> break
      }
    }
    
    return expr
  }
  
  private fun rest(): Node = when {
    match("LEFT_PAREN") -> node().also {
      consume("RIGHT_PAREN", "Expect ')' after expression.")
    }
    
    match("MINUS") -> BinaryNode("MINUS", ConstIntegerNode(0), rest())
    
    match("MOD") -> ModuleNode(node()).also {
      consume("MOD", "Expected '|' after expression")
    }
    
    match("INT") -> ConstIntegerNode(lastToken.value.toInt())
    match("DEC") -> {
      if (fp != null) {
        ConstFixedDecimalNode(
          BigDecimal(lastToken.value.toDouble(), MathContext(fp!!)), fp!!
        )
      } else {
        ConstDecimalNode(lastToken.value.toDouble())
      }
    }
    
    else -> when {
      match("E", "PI") -> SymbolNode(lastToken.value)
      match(
        "SIN", "COS", "TG", "CTG", "SQRT", "LN"
      ) -> SymbolNode(lastToken.value, mutableListOf(node()))
      
      match("LOG") -> SymbolNode(lastToken.value, mutableListOf(node(), node()))
      
      else -> error("Something went wrong with parsing? (${lastToken.value})")
    }
  }
  
  private fun consume(type: String, message: String): Token = if (match(type)) lastToken else error(message)
  private fun match(vararg types: String): Boolean = if (peek().type in types) true.also { advance() } else false
  private fun advance(): Token = code.removeFirst().also { lastToken = it }
  private fun error(message: String): Nothing = throw Error("[${lastToken.line} line] $message")
  private fun peek(): Token = if (code.isNotEmpty()) code[0] else getEOT()
  private fun getEOT(): Token = Token("EOT", "EOT", 3, lastToken.line)
}