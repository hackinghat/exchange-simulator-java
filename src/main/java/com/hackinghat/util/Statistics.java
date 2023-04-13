package com.hackinghat.util;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Statistics {


    public static double varianceP(double... samples)
    {
        return _variance(samples.length, samples);
    }

    public static double variance(double... samples)
    {
        return _variance(samples.length-1, samples);
    }

    public static double stdev(double... samples)
    {
        return Math.sqrt(variance(samples));
    }

    public static double stdevP(double... samples)
    {
        return Math.sqrt(varianceP(samples));
    }

    public static double[] apply(final Function<Double, Double> function, final double... samples)
    {
        return Arrays.stream(samples).map(function::apply).toArray();
    }

    public static double skewness(double... samples)
    {
        return moment(samples, 3.0, Statistics::_skewnessBias);
    }

    public static double kurtosis(double... samples)
    {
        return moment(samples, 4.0, Statistics::_kurtosisBias);
    }

    public static double skewness_(double... samples)
    {
        return moment(samples, 3.0, null);
    }

    public static double kurtosis_(double... samples)
    {
        return moment(samples, 4.0, null);
    }


    private static double moment(double[] samples, double moment, BiFunction<Double, Double, Double> biasCorrection)
    {
        final double mean = mean(samples);
        double moment_mean = mean(apply(x -> Math.pow(x - mean, moment), samples));
        double k0 = moment_mean / Math.pow(stdevP(samples), moment);
        return (biasCorrection== null) ? k0 : biasCorrection.apply(k0, (double)samples.length);
    }

    private static double _kurtosisBias(double k0, double n)
    {
        return 3.0 + (n-1)/((n-2)*(n-3)) * ((n+1)*k0 - 3*(n-1));
    }

    private static double _skewnessBias(double k0, double n)
    {
        return Math.sqrt(n*(n-1))/(n-2)*k0;
    }

    private static double _variance(int n, double... samples)
    {
        double mean = mean(samples);
        double result = 0.0;
        for (double sample : samples)
        {
            result += Math.pow(sample - mean, 2);
        }
        return result/n;
    }

    public static double mean(double... samples)
    {
        double result = 0.0;
        for (double sample : samples)
        {
            if (Double.isNaN(sample))
                throw new ArithmeticException("Unexpected NaN in samples");
            result += sample;
        }
        return result/samples.length;
    }
}
