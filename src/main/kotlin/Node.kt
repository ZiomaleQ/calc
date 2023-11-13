import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.*

sealed interface Node {
  fun prettyPrint(): String
  fun calc(): Node
  
  operator fun plus(other: Node): Node
  operator fun minus(other: Node): Node
  operator fun times(other: Node): Node
  operator fun div(other: Node): Node
  
  fun pow(other: Node): Node
}

class BinaryNode(var op: String, var left: Node, var right: Node) : Node {
  override fun toString() = "BinaryNode(op = '$op', left = '$left', right = '$right')"
  
  override fun prettyPrint(): String {
    return "${left.prettyPrint()} $op ${right.prettyPrint()}"
  }
  
  override fun plus(other: Node): Node = calc() + other
  override fun minus(other: Node): Node = calc() - other
  override fun times(other: Node): Node = calc() * other
  override fun div(other: Node): Node {
    if (other is ConstNode && other.expr.toDouble() == 0.0) {
      error("Cannot divide by 0 (zero)")
    }
    return calc() / other
  }
  
  override fun pow(other: Node): Node {
    val calculatedExpr = calc()
    val calculatedOther = other.calc()
    
    if (calculatedExpr is ConstNode && calculatedOther is ConstNode) {
      return ConstNode.numberToNode(calculatedExpr.expr.toDouble().pow(calculatedOther.expr.toDouble()))
    } else {
      error("Not calculated expression can't be used")
    }
  }
  
  override fun calc(): Node {
    val calculatedLeft = left.calc()
    val calculatedRight = right.calc()
    
    if (calculatedLeft is ConstNode && calculatedRight is ConstNode) {
      return when (op) {
        "PLUS" -> calculatedLeft + calculatedRight
        "MINUS" -> calculatedLeft - calculatedRight
        "STAR" -> calculatedLeft * calculatedRight
        "SLASH" -> calculatedLeft / calculatedRight
        "HAT" -> calculatedLeft.pow(calculatedRight)
        else -> error("Undefined operator? $op")
      }
    }
    
    return this
  }
}

class ModuleNode(private var expr: Node) : Node {
  override fun toString() = "Module(value = '$expr')"
  
  override fun prettyPrint(): String {
    return "|$expr|"
  }
  
  override fun calc(): Node {
    return when (val calculatedExpr = expr.calc()) {
      is ConstNode -> ConstNode.numberToNode(abs(calculatedExpr.expr.toDouble()))
      else -> error("Not calculated expression can't be used")
    }
  }
  
  override fun plus(other: Node): Node = calc() + other
  override fun minus(other: Node): Node = calc() - other
  override fun times(other: Node): Node = calc() * other
  override fun div(other: Node): Node = calc() / other
  
  override fun pow(other: Node): Node {
    val calculatedExpr = expr.calc()
    
    return calculatedExpr.pow(other)
  }
  
}

sealed class ConstNode(var expr: Number) : Node {
  override fun prettyPrint(): String = "$expr"
  
  override fun calc(): Node = this
  
  companion object {
    fun numberToNode(number: Number): ConstNode {
      val value = number.toDouble()
      val roundedValue = value.toInt()
      
      if (fp != null) {
        return ConstFixedDecimalNode(BigDecimal(value, MathContext(fp!!)), fp!!)
      }
      
      return if (value == roundedValue.toDouble()) {
        ConstIntegerNode(roundedValue)
      } else {
        ConstDecimalNode(value)
      }
    }
  }
}

class ConstIntegerNode(expr: Int) : ConstNode(expr) {
  override operator fun plus(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstIntegerNode(expr as Int + other.expr as Int)
    is ConstDecimalNode -> ConstDecimalNode(expr.toDouble() + other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble() + other.expr.toDouble(), MathContext(other.precision)), other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this + other.calc()
  }
  
  override operator fun minus(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstIntegerNode(expr as Int - other.expr as Int)
    is ConstDecimalNode -> ConstDecimalNode(expr.toDouble() - other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble(), MathContext(other.precision)) - other.expr as BigDecimal, other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this - other.calc()
  }
  
  override operator fun times(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstIntegerNode(expr as Int * other.expr as Int)
    is ConstDecimalNode -> ConstDecimalNode(expr.toDouble() * other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble(), MathContext(other.precision)) * other.expr as BigDecimal, other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this * other.calc()
  }
  
  override operator fun div(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstDecimalNode(expr.toDouble() / other.expr.toDouble())
    is ConstDecimalNode -> ConstDecimalNode(expr.toDouble() / other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble(), MathContext(other.precision)) / other.expr as BigDecimal, other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this / other.calc()
  }
  
  override fun pow(other: Node): Node {
    val calculatedOther = other.calc()
    if (calculatedOther !is ConstNode) {
      error("Not calculated expression cant be used")
    }
    val value = expr.toDouble().pow(calculatedOther.expr.toDouble())
    
    return numberToNode(value)
  }
  
}

class ConstDecimalNode(expr: Double) : ConstNode(expr) {
  override operator fun plus(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstDecimalNode(expr as Double + other.expr.toDouble())
    is ConstDecimalNode -> ConstDecimalNode(expr as Double + other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble() + other.expr.toDouble(), MathContext(other.precision)), other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this + other.calc()
  }
  
  override operator fun minus(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstDecimalNode(expr as Double - other.expr.toDouble())
    is ConstDecimalNode -> ConstDecimalNode(expr as Double - other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble() - other.expr.toDouble(), MathContext(other.precision)), other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this - other.calc()
  }
  
  override fun times(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstDecimalNode(expr as Double * other.expr.toDouble())
    is ConstDecimalNode -> ConstDecimalNode(expr as Double * other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble() * other.expr.toDouble(), MathContext(other.precision)), other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this * other.calc()
  }
  
  override fun div(other: Node): Node = when (other) {
    is ConstIntegerNode -> ConstDecimalNode(expr as Double / other.expr.toDouble())
    is ConstDecimalNode -> ConstDecimalNode(expr as Double / other.expr as Double)
    is ConstFixedDecimalNode -> ConstFixedDecimalNode(
      BigDecimal(expr.toDouble() / other.expr.toDouble(), MathContext(other.precision)), other.precision
    )
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this / other.calc()
  }
  
  override fun pow(other: Node): Node {
    val calculatedOther = other.calc()
    if (calculatedOther !is ConstNode) {
      error("Not calculated expression cant be used")
    }
    val value = expr.toDouble().pow(calculatedOther.expr.toDouble())
    
    return numberToNode(value)
  }
}

class ConstFixedDecimalNode(expr: BigDecimal, var precision: Int) : ConstNode(expr) {
  override operator fun plus(other: Node): Node = when (other) {
    is ConstIntegerNode -> {
      var newValue = ((expr as BigDecimal) + (other.expr as Int).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstDecimalNode -> {
      var newValue = ((expr as BigDecimal) + (other.expr as Double).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstFixedDecimalNode -> {
      var newValue = ((expr as BigDecimal) + (other.expr as BigDecimal))
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this + other.calc()
  }
  
  override operator fun minus(other: Node): Node = when (other) {
    is ConstIntegerNode -> {
      var newValue = ((expr as BigDecimal) - (other.expr as Int).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstDecimalNode -> {
      var newValue = ((expr as BigDecimal) - (other.expr as Double).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstFixedDecimalNode -> {
      var newValue = ((expr as BigDecimal) - (other.expr as BigDecimal))
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this - other.calc()
  }
  
  override fun times(other: Node): Node = when (other) {
    is ConstIntegerNode -> {
      var newValue = ((expr as BigDecimal) * (other.expr as Int).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstDecimalNode -> {
      var newValue = ((expr as BigDecimal) * (other.expr as Double).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstFixedDecimalNode -> {
      var newValue = ((expr as BigDecimal) * (other.expr as BigDecimal))
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this * other.calc()
  }
  
  override fun div(other: Node): Node = when (other) {
    is ConstIntegerNode -> {
      var newValue = ((expr as BigDecimal) / (other.expr as Int).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstDecimalNode -> {
      var newValue = ((expr as BigDecimal) / (other.expr as Double).toBigDecimal())
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is ConstFixedDecimalNode -> {
      var newValue = ((expr as BigDecimal) / (other.expr as BigDecimal))
      
      if (fp != null) {
        newValue = newValue.setScale(fp!!, RoundingMode.DOWN)
      }
      
      ConstFixedDecimalNode(newValue, precision)
    }
    
    is BinaryNode, is ModuleNode, is SymbolNode -> this / other.calc()
  }
  
  override fun pow(other: Node): Node {
    val calculatedOther = other.calc()
    if (calculatedOther !is ConstNode) {
      error("Not calculated expression cant be used")
    }
    val value = expr.toDouble().pow(calculatedOther.expr.toDouble())
    
    return numberToNode(value)
  }
}

class SymbolNode(private var symbol: String, private var expression: MutableList<Node> = mutableListOf()) : Node {
  override fun toString() = if (expression.isEmpty()) symbol else "$symbol(${expression.joinToString()})"
  
  override fun prettyPrint(): String {
    return if (expression.isEmpty()) symbol else "$symbol ${expression.joinToString(" ")}"
  }
  
  override fun calc(): Node = when (symbol) {
    "sin" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(sin(calculatedExpr.expr.toDouble()))
    }
    
    "cos" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(cos(calculatedExpr.expr.toDouble()))
    }
    
    "tg" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(tan(calculatedExpr.expr.toDouble()))
    }
    
    "ctg" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(1 / (calculatedExpr.expr.toDouble()))
    }
    
    "sqrt" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      } else {
        if(calculatedExpr.expr.toDouble() < 0) {
          error("Negative square roots are not supported")
        }
      }
      
      ConstNode.numberToNode(sqrt(calculatedExpr.expr.toDouble()))
    }
    
    "ln" -> {
      val calculatedExpr = expression[0].calc()
      
      if (calculatedExpr !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(ln(calculatedExpr.expr.toDouble()))
    }
    
    "log" -> {
      val calculatedExpr = expression[0].calc()
      val calculatedSecond = expression[1].calc()
      
      if (calculatedExpr !is ConstNode || calculatedSecond !is ConstNode) {
        error("Not calculated expression cant be used")
      }
      
      ConstNode.numberToNode(log(calculatedSecond.expr.toDouble(), calculatedExpr.expr.toDouble()))
    }
    
    "pi" -> ConstNode.numberToNode(Math.PI)
    "e" -> ConstNode.numberToNode(Math.E)
    
    else -> error("Unknown symbol")
  }
  
  override fun plus(other: Node): Node = calc() + other
  override fun minus(other: Node): Node = calc() - other
  override fun times(other: Node): Node = calc() * other
  override fun div(other: Node): Node = calc() / other
  
  override fun pow(other: Node): Node {
    val calculatedExpr = calc()
    val calculatedOther = other.calc()
    
    if (calculatedExpr is ConstNode && calculatedOther is ConstNode) {
      return ConstDecimalNode(calculatedExpr.expr.toDouble().pow(calculatedOther.expr.toDouble()))
    } else {
      error("Not calculated expression can't be used")
    }
  }
}