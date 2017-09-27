package eu.timepit.refined
package macros

import eu.timepit.refined.api.{Refined, RefType, Validate}
import eu.timepit.refined.internal.Resources
import macrocompat.bundle
import scala.reflect.macros.blackbox
import shapeless.tag.@@

@bundle
class RefineMacro(val c: blackbox.Context) extends MacroUtils {
  import c.universe._

  def impl[F[_, _], T: c.WeakTypeTag, P: c.WeakTypeTag](t: c.Expr[T])(
      rt: c.Expr[RefType[F]],
      v: c.Expr[Validate[T, P]]
  ): c.Expr[F[T, P]] = {

    val tValue: T = t.tree match {
      case Literal(Constant(value)) => value.asInstanceOf[T]
      case _                        => abort(Resources.refineNonCompileTimeConstant)
    }

    val validate = eval(v)
    val res = validate.validate(tValue)
    if (res.isFailed) {
      abort(validate.showResult(tValue, res))
    }

    refTypeObj(rt).unsafeWrapM(c)(t)
  }

  def implApplyRef[FTP, F[_, _], T, P](t: c.Expr[T])(
      ev: c.Expr[F[T, P] =:= FTP],
      rt: c.Expr[RefType[F]],
      v: c.Expr[Validate[T, P]]
  ): c.Expr[FTP] =
    c.Expr(impl(t)(rt, v).tree)

  private def refTypeObj[F[_, _]](rt: c.Expr[RefType[F]]): RefType[F] =
    if (rt.tree.tpe =:= weakTypeOf[RefType[Refined]])
      RefType[Refined].asInstanceOf[RefType[F]]
    else if (rt.tree.tpe =:= weakTypeOf[RefType[@@]])
      RefType[@@].asInstanceOf[RefType[F]]
    else
      eval(rt)
}
