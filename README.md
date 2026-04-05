# Robocode Ensemble: Reactive Navigation Framework

This repository houses a collection of autonomous agents and a custom navigation framework built for the [Robocode](https://robocode.sourceforge.io/) environment. 

Rather than focusing on static pathfinding, this project explores **Ensemble Logic**—a perspective on system design that prioritizes real-time environmental "listening" over rigid, pre-determined planning.

## 🧠 The Core Concept: Coding for Tendencies

The primary technical differentiator in this framework is the move away from fixed destinations. In a high-velocity environment, a fixed destination is a commitment to a future that is often obsolete by the time movement begins.

### **WayPointMovement.java**
The heart of this repository is the `WayPointMovement` engine. It implements a **Decoupled Navigation Strategy** where:
* **Mechanism is separated from Intent:** The robot doesn't "go to a point"; it evaluates a field of potential waypoints.
* **Tactile Re-planning:** Using weighted environmental heuristics (enemy proximity, technical "friction" of walls, and attractive "gravity" of the center), the agent can drop a previous destination instantly to pivot toward a more optimal equilibrium.

## 🛠️ Key Components

* **`DiscoRobot.java`**: An abstract base class that provides a modular SDK for building complex robot "personalities" (Dodge, Attack, Patrol) on top of a shared utility engine.
* **`CircularIntercept.java`**: A non-linear path prediction engine used to calculate target intercepts based on angular velocity and change-in-heading over time.
* **`Bespin.java`**: A reference bot utilizing **Anti-Gravity Movement**, where every entity on the battlefield exerts a mathematical force—repulsive or attractive—to determine the agent's path.

## 🎵 The Connection: Ensemble Logic

This technical framework is a direct implementation of my philosophy on orchestration. Whether managing a 40-piece orchestra or a distributed system at scale, success is found in the ability to "listen to the room" and adjust the performance to the present reality.

---
*For more on the philosophy behind this code, read the essay: [Listening to the Room](https://danonkeys.com/posts/listening-to-the-room)*
