import java.util.Scanner

var fp: Int? = null

fun main() {
  println("Set floating point precision by writing 'fp={integer}' at the start of the line")
  
  while (true) {
    print("> ")
    
    var input = readln()
    
    if (input.startsWith("fp=")) {
      val scan = Scanner(input.substring("fp=".length))
      try {
        fp = scan.nextInt()
        
        println("Setting precision to ${fp ?: 1}")
      } catch (err: RuntimeException) {
        println("Couldn't find valid fixed precision value")
        if (fp != null) {
          println("Resetting precision")
          fp = null
        }
        continue
      }
      
      try {
        input = scan.nextLine()
      } catch (err: RuntimeException) {
        continue
      }
    }
    
    val tokens = scanTokens(input)
    
    val expressions = Parser(tokens).parse()
    
    if (expressions.size == 1) {
      val output = expressions[0].calc()
      println(output.prettyPrint())
    } else {
      println("ERROR: More than one expression?")
    }
  }
}