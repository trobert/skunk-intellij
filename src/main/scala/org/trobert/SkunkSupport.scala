package org.trobert

import com.intellij.psi.PsiClass
import org.jetbrains.plugins.scala.lang.macros.evaluator.{MacroImpl, MacroInvocationContext, ScalaMacroExpandable}
import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType
import org.jetbrains.plugins.scala.lang.psi.types.api.{Any, Nothing}
import org.jetbrains.plugins.scala.lang.psi.types.{ScExistentialArgument, ScExistentialType, ScParameterizedType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

class SkunkSupport extends ScalaMacroExpandable {
  override val boundMacro: Seq[MacroImpl] = MacroImpl("sql", "skunk.syntax.StringContextOps") :: Nil

  override def expandMacro(macros: ScFunction, context: MacroInvocationContext): Option[ScExpression] = {
    val scope: ElementScope = context.call.elementScope

    for {
      encoderClass    <- scope.getCachedClass("skunk.Encoder")
      encoderType      = wildcardType(encoderClass)
      fragmentClass   <- scope.getCachedClass("skunk.Fragment")
      fragmentType     = wildcardType(fragmentClass)
      voidClass       <- scope.getCachedClass("skunk.Void")
      voidFragmentType = ScParameterizedType(ScDesignatorType(fragmentClass), List(ScDesignatorType(voidClass)))
    } yield {
      def encoderFor(expression: ScExpression, scType: ScType) =
        if (scType.conforms(encoderType)) Some(expression.getText)
        else if (scType.conforms(voidFragmentType)) None
        else if (scType.conforms(fragmentType)) Some(s"${expression.getText}.encoder")
        else None

      val encoders = for {
        arg <- context.call.argumentExpressions
        typ <- arg.`type`().toOption
        enc <- encoderFor(arg, typ)
      } yield enc

      val encoder = encoders.reduceLeftOption((a, b) => s"$a ~ $b").getOrElse("_root_.skunk.Void.codec")
      ScalaPsiElementFactory.createExpressionWithContextFromText(
        s"""_root_.skunk.Fragment(List.empty, $encoder, null)""",
        context.call,
        null
      )
    }
  }

  private def wildcardType(psiClass: PsiClass) = {
    implicit val context: ProjectContext = psiClass
    ScExistentialType(ScParameterizedType(ScDesignatorType(psiClass), List(ScExistentialArgument("_$1", List.empty, Nothing, Any))))
  }
}
