The simulator is written in Java

Prerequisite:
Java version of 14 or higher is needed

How to run
1. Compile the simulator by following command in src
	> javac Main.java

2. Run the simulator by following command with 7 arguments
	> java Main <pipeline-width> <branch-mode> <num-of-alu> <num-of-load> <num-of-store> <num-of-bru> <rs-size>
	
Arguments information:
	<pipeline-width>: Superscalar pipeline width (min: 1)
	<branch-mode>: Branch prediction mode (Fixed-not-taken: 1, Fixed-taken: 2, Static: 3, 1-bit-dynamic: 4, 2-bit-dynamic: 5)
	<num-of-alu>: Number of ALUs (min: 1)
	<num-of-load>: Number of Load units (min: 1)
	<num-of-store>: Number of Store units (min: 1)
	<num-of-bru>: Number of BRUs (min: 1)
	<rs-size>: Size of Reservation station and Reorder buffer (min: 1)

Output
The simulator will run 6 benchmarking programs
1. Vector addition (size: 10)
2. Bubble sort (size: 16)
3. Factorial(8)
4. Many dependencies
5. Independent Math
6. 3 x 3 Game of Life (1 iteration)

For each program, terminal will show following infromation
0.  Processor configuration
1.  Number of instructions executed
2.  Number of cycles spent
3.  Number of stalled cycles
4.  Number of waiting cycles
5.  Number of correct branch predictions
6.  Number of incorrect branch predictions
7.  Number of instructions per cycle
8.  Subset instructions per cycle (instructions per cycle when last halt was issued)
9.  Stalled cycles per cycle
10. Wasted cycles per cycle (wasted cycle = stalled + waiting cycle)
11. Branch prediction accuracy

Also, there will be register file and memory dump for each program in following format
1. rf_bench#.txt
2. memory_bench#.txt

