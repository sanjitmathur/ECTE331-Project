# ECTE331 - Real-Time Embedded Systems Project

Java implementations for ECTE331 coursework.

## Part 1 - Fault-Tolerant Drone Navigation
Triple Modular Redundancy (TMR) system with three altitude sensors, majority voting, and SAFE MODE.

**Run:**
```
cd Part1_DroneNavigation
javac *.java
java DroneNavigationSystem
```

## Part 2 - Real-Time Robotic Arm Controller
Multi-threaded robotic arm simulation covering synchronisation, priority inversion, priority inheritance, and priority ceiling protocols.

**Run (compile once, then run any task):**
```
cd Part2_RoboticArm
javac *.java
java Task1_BasicImpl
java Task2_Synchronization
java Task3_PriorityInversion
java Task4_PriorityInheritance
java Task5_PriorityCeiling
java Task6_Performance
```

## Part B - Thread Synchronisation and Communication
Two cooperating threads with semaphore-based dependency enforcement, verified over 1000 iterations.

**Run:**
```
cd PartB_ThreadSync
javac *.java
java ThreadSyncApp
```
