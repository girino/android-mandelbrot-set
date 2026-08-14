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
        
        public void set(double R) {
        	this.set(R, 0);
        }
        public void set(Complex B) {
        	this.set(B.real, B.imag);
        }
        public void set(double R, double I) {
        	this.real = R;
        	this.imag = I;
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
        
        public void ltimes(Complex A, Complex B) {
        	this.set(A.real * B.real - A.imag * B.imag,A.real * B.imag + B.real * A.imag);
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
        public void ladd(Complex A, Complex B) {
            this.set(A.real + B.real, A.imag + B.imag);
        }
        public void lsub(Complex A, Complex B) {
            this.set(A.real - B.real, A.imag - B.imag);
        }

        /**
         * complex square 
         * @param A
         * @return
         */
        public static Complex square(Complex A) {
            return Complex.times(A,A);
        }
        
        public void square() {
            this.ltimes(this,this);
        }

        public void lsquare(Complex A) {
            this.ltimes(A,A);
        }

        /**
         * complex modulus.
         * @param A
         * @return
         */
        public static double modulus(Complex A) {
            return Math.sqrt(A.real * A.real + A.imag * A.imag);
        }
        public double modulus() {
            return Math.sqrt(this.real * this.real + this.imag * this.imag);
        }
        public static double modulus2(Complex A) {
            return A.real * A.real + A.imag * A.imag;
        }
        public double modulus2() {
            return this.real * this.real + this.imag * this.imag;
        }
        
        public static Complex minus(Complex A) {
        	return new Complex(-A.real, -A.imag);
        }
        public void minus() {
        	this.set(-this.real, -this.imag);
        }
        public void lminus(Complex A) {
        	this.set(-A.real, -A.imag);
        }
        
        public static Complex conjugate(Complex A) {
        	return new Complex(A.real, -A.imag);
        }
        public void lconjugate(Complex A) {
        	this.set(A.real, -A.imag);
        }
        public void conjugate() {
        	this.set(this.real, -this.imag);
        }
        
        public static Complex inverse(Complex A) {
        	double denom = A.real * A.real + A.imag * A.imag;
        	return new Complex(A.real/denom, - A.imag / denom);
        }
        public void linverse(Complex A) {
        	double denom = A.real * A.real + A.imag * A.imag;
        	this.set(A.real/denom, - A.imag / denom);
        }
        public void inverse() {
        	Complex A = this;
        	double denom = A.real * A.real + A.imag * A.imag;
        	this.set(A.real/denom, - A.imag / denom);
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

		public void abs() {
			this.real = Math.abs(this.real);
			this.imag = Math.abs(this.imag);
		}
}