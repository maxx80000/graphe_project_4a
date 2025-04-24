# Aircraft Landing Problem (ALP) Optimization

This project implements various formulations of the Aircraft Landing Problem (ALP) using CPLEX optimization library.

## Overview

The Aircraft Landing Problem involves scheduling aircraft landings on available runways while respecting safety constraints and optimizing various objectives.

This implementation includes three problem variants:
1. **Problem 1**: Minimizing Weighted Delay with Target Landing Times
2. **Problem 2**: Minimizing Makespan (completion time of the last landing)
3. **Problem 3**: Minimizing Total Lateness with Runway Assignment

## Setup and Requirements

### Prerequisites

- Java JDK 11 or higher
- Maven
- IBM CPLEX Optimization Studio (tested with version 20.1.0)

### Installation

1. Clone this repository
2. Install CPLEX and copy the `cplex.jar` file to the `lib/` directory in the project root
3. Build the project using Maven:

```
mvn clean package
```

## Usage

1. Download instance files from the OR-Library: https://people.brunel.ac.uk/~mastjjb/jeb/orlib/airlandinfo.html
2. Create a directory named `instances` in the project root
3. Place the OR-Library instance files in the `instances` directory
4. Run the application:

```
java -jar target/aircraft-landing-problem-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Output

The program will:
- Solve each problem variant for each instance with different runway counts
- Create a `results` directory with detailed analysis for each solution
- Generate a `summary.txt` file with summary statistics
- Optionally visualize the schedules (can be enabled by uncommenting the visualization code in Main.java)

## Project Structure

- `src/main/java/alp/model`: Data model classes
- `src/main/java/alp/io`: I/O utilities for reading instance files
- `src/main/java/alp/solver`: CPLEX-based solvers for each problem variant
- `src/main/java/alp/analysis`: Analysis utilities
- `src/main/java/alp/visualization`: Schedule visualization utilities
- `src/main/java/alp/Main.java`: Main class for running the application

## Implementation Details

Each problem variant is implemented using a different mathematical formulation:

1. **Problem 1** (Minimizing Weighted Delay):
   - Decision variables for landing times and runway assignments
   - Variables for early and late penalties
   - Constraints for time windows, runway assignments, and separation times

2. **Problem 2** (Minimizing Makespan):
   - Similar structure to Problem 1
   - Objective function focuses on minimizing the maximum landing time

3. **Problem 3** (Minimizing Total Lateness):
   - Additional consideration of transfer times from runway to parking
   - Objective function minimizes the sum of lateness at parking positions

## License

This project is provided for educational purposes only.
