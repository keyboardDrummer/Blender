class Fibonacci
{
    public static void main(java.lang.String[] args)
    {
        System.out.print(Fibonacci.fibonacci(5));
    }

    public static int fibonacci(int index)
    {
        return index < 2 ? 1 : Fibonacci.fibonacci(index - 1) + Fibonacci.fibonacci(index - 2);
    }
}