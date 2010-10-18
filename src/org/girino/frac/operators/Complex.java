package org.girino.frac.operators;

/**
 * Complex number. 
 * @author girino
 *
 */
public class Complex {
        double real;
        double imag;

        /**
         * deffaults to 0 + 0i.
         *
         */
        public Complex() {
                real = 0;
                imag = 0;
        }

        /**
         * creates with only a real part R => R + 0i
         */
        public Complex(double R) {
                real = R;
                imag = 0;
        }

        /**
         * Creates with real and imaginary parts => R + Ii.
         * @param R
         * @param I
         */
        public Complex(double R, double I) {
                real = R;
                imag = I;
        }

        /**
         * complex multiplication.
         * @param A
         * @param B
         * @return
         */
        public static Complex times(Complex A, Complex B) {
                return new Complex(A.real * B.real - A.imag * B.imag,A.real * B.imag + B.real * A.imag);
        }
 
        /**
         * complex sum.
         * @param A
         * @param B
         * @return
         */
        public static Complex add(Complex A, Complex B) {
                return new Complex(A.real + B.real, A.imag + B.imag);
        }

        /**
         * complex square 
         * @param A
         * @return
         */
        public static Complex square(Complex A) {
                return Complex.times(A,A);
        }

        /**
         * complex modulus.
         * @param A
         * @return
         */
        public static double modulus(Complex A) {
                return Math.sqrt(A.real * A.real + A.imag * A.imag);
        }
        
        public static Complex minus(Complex A) {
        	return new Complex(-A.real, -A.imag);
        }
        
        public static Complex conjugate(Complex A) {
        	return new Complex(A.real, -A.imag);
        }
        
        public static Complex inverse(Complex A) {
        	double denom = A.real * A.real + A.imag * A.imag;
        	return new Complex(A.real/denom, - A.imag / denom);
        }

		public double getImag() {
			return imag;
		}

		public double getReal() {
			return real;
		}
		
		public String toString() {
			return "{" + this.real + ", " + this.imag + "}";
		}
}

