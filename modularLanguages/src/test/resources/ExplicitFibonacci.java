import java.lang.*;
class Fibonacci extends Object
{
    public void <init>()
    {
        super();
        return;
    }

    public static void main(java.lang.String[] args)
    {
        System.out.print(Fibonacci.fibonacci(5));
        return;
    }

    public static int fibonacci(int index)
    {
        return index < 2 ? 1 : Fibonacci.fibonacci(index - 1) + Fibonacci.fibonacci(index - 2);
    }
}