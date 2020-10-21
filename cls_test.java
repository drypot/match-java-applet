
class cls_object
{
   String print = "hahaha";

   public String toString()
   {
      return "Hello?";
   }

   public void print()
   {
      System.out.println(print);
   }
}

public class cls_test
{
   public static void main(String[] args)
   {
      cls_object object = new cls_object();
      Object obj;

      object.print();
      obj = object;
      ((cls_object)obj).print();
   }
}
