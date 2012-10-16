package sqltyped

import shapeless._

final class AssocHListOps[L <: HList](l: L) {
  def lookup[K](implicit lookup: Lookup[L, K]): lookup.Out = lookup(l)

  def get[K](k: K)(implicit lookup0: Lookup[L, K]): lookup0.Out = lookup[K]

  def removeKey[K](k: K)(implicit remove: RemoveKey[L, K]): remove.Out = remove(l)

  def renameKey[K1, K2](oldKey: K1, newKey: K2)(implicit rename: RenameKey[L, K1, K2]): rename.Out = rename(l, newKey)

  def modify[K, A, B](k: K)(f: A => B)(implicit modify: Modify[L, K, A, B]): modify.Out = modify(l, f)

  def values(implicit valueProj: ValueProjection[L]): valueProj.Out = valueProj(l)
  def values0[Out <: HList](implicit valueProj: ValueProjectionAux[L, Out]): Out = valueProj(l)
}

final class ListOps[L <: HList](l: List[L]) {
  def values(implicit valueProj: ValueProjection[L]): List[valueProj.Out] = l.map(_.values)

  def tuples[Out0 <: HList, Out <: Product]
    (implicit 
       valueProj: ValueProjectionAux[L, Out0],
       tupler: TuplerAux[Out0, Out]) = l.map(_.values0.tupled)
}

final class OptionOps[L <: HList](o: Option[L]) {
  def values(implicit valueProj: ValueProjection[L]): Option[valueProj.Out] = o.map(_.values)

  def tuples[Out0 <: HList, Out <: Product]
    (implicit 
       valueProj: ValueProjectionAux[L, Out0],
       tupler: TuplerAux[Out0, Out]) = o.map(_.values0.tupled)
}

@annotation.implicitNotFound(msg = "No such key ${K}")
trait Lookup[L <: HList, K] {
  type Out
  def apply(l: L): Out
}

object Lookup {
  implicit def hlistLookup1[K, V, T <: HList] = new Lookup[(K, V) :: T, K] {
    type Out = V
    def apply(l: (K, V) :: T) = l.head._2
  }

  implicit def hlistLookup[K, V, T <: HList, K1, V1](implicit st: Lookup[T, K1]) = new Lookup[(K, V) :: T, K1] {
    type Out = st.Out
    def apply(l: (K, V) :: T) = st(l.tail)
  }
}

@annotation.implicitNotFound(msg = "No such key ${K}")
trait RemoveKey[L <: HList, K] {
  type Out
  def apply(l: L): Out
}

trait RemoveKeyAux[L <: HList, K, Rem <: HList] {
  def apply(l: L): Rem
}

object RemoveKey {
  implicit def hlistRemoveKey[L <: HList, K, Rem <: HList](implicit aux: RemoveKeyAux[L, K, Rem]) = new RemoveKey[L, K] {
    type Out = Rem
    def apply(l: L): Rem = aux(l)
  }
}

object RemoveKeyAux {
  implicit def hlistRemoveKey1[K, V, T <: HList] = new RemoveKeyAux[(K, V) :: T, K, T] {
    def apply(l: (K, V) :: T): T = l.tail
  }
  
  implicit def hlistRemoveKey[H, T <: HList, K, Rem <: HList](implicit r: RemoveKeyAux[T, K, Rem]) =
    new RemoveKeyAux[H :: T, K, H :: Rem] {
      def apply(l: H :: T): H :: Rem = l.head :: r(l.tail)
    }
}

@annotation.implicitNotFound(msg = "No such key ${K1}")
trait RenameKey[L <: HList, K1, K2] {
  type Out
  def apply(l: L, newKey: K2): Out
}

trait RenameKeyAux[L <: HList, K1, K2, Rem <: HList] {
  def apply(l: L, newKey: K2): Rem
}

object RenameKey {
  implicit def hlistRenameKey[L <: HList, K1, K2, Rem <: HList](implicit aux: RenameKeyAux[L, K1, K2, Rem]) = new RenameKey[L, K1, K2] {
    type Out = Rem
    def apply(l: L, newKey: K2): Rem = aux(l, newKey)
  }
}

object RenameKeyAux {
  implicit def hlistRenameKey1[K1, K2, V, T <: HList] = new RenameKeyAux[(K1, V) :: T, K1, K2, (K2, V) :: T] {
    def apply(l: (K1, V) :: T, newKey: K2): (K2, V) :: T = (newKey, l.head._2) :: l.tail
  }
  
  implicit def hlistRenameKey[H, T <: HList, K1, K2, Rem <: HList](implicit r: RenameKeyAux[T, K1, K2, Rem]) =
    new RenameKeyAux[H :: T, K1, K2, H :: Rem] {
      def apply(l: H :: T, newKey: K2): H :: Rem = l.head :: r(l.tail, newKey)
    }
}

@annotation.implicitNotFound(msg = "No key ${K} with value of type ${A}")
trait Modify[L <: HList, K, A, B] {
  type Out
  def apply(l: L, f: A => B): Out
}

trait ModifyAux[L <: HList, K, A, B, Rem <: HList] {
  def apply(l: L, f: A => B): Rem
}

object Modify {
  implicit def hlistModify[L <: HList, K, A, B, Rem <: HList](implicit aux: ModifyAux[L, K, A, B, Rem]) = new Modify[L, K, A, B] {
    type Out = Rem
    def apply(l: L, f: A => B): Rem = aux(l, f)
  }
}

object ModifyAux {
  implicit def hlistModify1[K, A, B, T <: HList] = new ModifyAux[(K, A) :: T, K, A, B, (K, B) :: T] {
    def apply(l: (K, A) :: T, f: A => B): (K, B) :: T = (l.head._1, f(l.head._2)) :: l.tail
  }
  
  implicit def hlistModify[H, T <: HList, K, A, B, Rem <: HList](implicit r: ModifyAux[T, K, A, B, Rem]) =
    new ModifyAux[H :: T, K, A, B, H :: Rem] {
      def apply(l: H :: T, f: A => B): H :: Rem = l.head :: r(l.tail, f)
    }
}

trait ValueProjection[L <: HList] {
  type Out <: HList
  def apply(l: L): Out
}

trait ValueProjectionAux[L <: HList, Out <: HList] {
  def apply(l: L): Out
}

object ValueProjection {
  implicit def valueProjection[L <: HList, Out0 <: HList](implicit vp: ValueProjectionAux[L, Out0]) = new ValueProjection[L] {
    type Out = Out0
    def apply(l: L): Out = vp(l)
  }
}

object ValueProjectionAux {
  implicit def valueProjectionHNil[K, V] = new ValueProjectionAux[(K, V) :: HNil, V :: HNil] {
    def apply(x: (K, V) :: HNil) = x.head._2 :: HNil
  }

  implicit def valueProjection[K, V, T <: HList](implicit st: ValueProjection[T]) = new ValueProjectionAux[(K, V) :: T, V :: st.Out] {
    def apply(x: (K, V) :: T) = x.head._2 :: st(x.tail)
  }
}
