#region Using Directives

using System;
using System.Linq.Expressions;
using System.Reflection;


#endregion

namespace SLaB.Utilities
{
    /// <summary>
    ///   Provides a set of methods that can be used to simplify reflection operations.
    /// </summary>
    public static class ReflectionUtilities
    {
        /// <summary>
        ///   Gets the FieldInfo for a property access in the expression passed in.  Also works for private fields.
        /// </summary>
        /// <typeparam name = "TTargetType">The delegate type from which to get the expression.</typeparam>
        /// <param name = "expression">The expression to use to get the FieldInfo.</param>
        /// <returns>The FieldInfo for the last field access in the expression.</returns>
        public static FieldInfo GetFieldInfo<TTargetType>(Expression<Func<TTargetType, object>> expression)
        {
            LambdaExpression expr = (LambdaExpression)expression;
            if (expr.Body.NodeType != ExpressionType.MemberAccess)
                throw new ArgumentException();
            MemberExpression mcExpr = (MemberExpression)expr.Body;
            return mcExpr.Member as FieldInfo;
        }

        /// <summary>
        ///   Gets the MethodInfo for a method call in the expression passed in.  Also works for private methods.
        /// </summary>
        /// <typeparam name = "TTargetType">The delegate type from which to get the expression.</typeparam>
        /// <param name = "expression">The expression to use to get the MethodInfo.</param>
        /// <returns>The MethodInfo for the last method call in the expression.</returns>
        public static MethodInfo GetMethodInfo<TTargetType>(Expression<Action<TTargetType>> expression)
        {
            LambdaExpression expr = (LambdaExpression)expression;
            if (expr.Body.NodeType != ExpressionType.Call)
                throw new ArgumentException();
            MethodCallExpression mcExpr = (MethodCallExpression)expr.Body;
            return mcExpr.Method;
        }

        /// <summary>
        ///   Gets the PropertyInfo for a property access in the expression passed in.  Also works for private properties.
        /// </summary>
        /// <typeparam name = "TTargetType">The delegate type from which to get the expression.</typeparam>
        /// <param name = "expression">The expression to use to get the PropertyInfo.</param>
        /// <returns>The PropertyInfo for the last property access in the expression.</returns>
        public static PropertyInfo GetPropertyInfo<TTargetType>(Expression<Func<TTargetType, object>> expression)
        {
            LambdaExpression expr = (LambdaExpression)expression;
            if (expr.Body.NodeType != ExpressionType.MemberAccess)
                throw new ArgumentException();
            MemberExpression mcExpr = (MemberExpression)expr.Body;
            return mcExpr.Member as PropertyInfo;
        }

    }
}