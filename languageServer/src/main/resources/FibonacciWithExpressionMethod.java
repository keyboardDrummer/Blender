class FibonacciWithExpressionMethod
{
  public static void main(java.lang.String[] args)
  {
    System.out.print(fibonacci(5));
  }

  public static int fibonacci(int index) = index < 2 ? 1 : fibonacci(index - 1) + fibonacci(index - 2)
}