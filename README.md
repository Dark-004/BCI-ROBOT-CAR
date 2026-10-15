# 🚗 Neuro-Drive — Bio-Signal Controlled Smart Car

### Driving Innovation Through Brain and Muscle Signals 🧠

A bio-signal-controlled robotic car that explores hands-free vehicle navigation using EEG and EMG signals, embedded systems, and wireless communication.

**Neuro-Drive** is a mini project that integrates biosignal acquisition, microcontroller programming, and motor control to demonstrate an alternative approach to human–machine interaction.

---

## ✨ Project Highlights

- 🧠 **Bio-Signal Input:** Uses EEG and EMG sensor modules to acquire electrical biosignals.
- 🎮 **Hands-Free Control:** Explores vehicle movement control through processed biosignal inputs.
- ⚡ **Embedded Systems:** Uses an ESP32 microcontroller for hardware control.
- 📡 **Wireless Communication:** Supports Bluetooth-based communication between the control application and the car.
- 🔧 **Motor Control:** Uses an L298N motor driver to control the DC motors.
- 🤖 **Human–Machine Interaction:** Demonstrates the integration of biosignals with a physical robotic system.

## 🛠️ Technology Stack

![C++](https://img.shields.io/badge/C%2B%2B-00599C?style=for-the-badge&logo=cplusplus&logoColor=white)
![Arduino](https://img.shields.io/badge/Arduino-00979D?style=for-the-badge&logo=arduino&logoColor=white)
![ESP32](https://img.shields.io/badge/ESP32-Microcontroller-E7352C?style=for-the-badge)
![Bluetooth](https://img.shields.io/badge/Bluetooth-Wireless-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)
![Embedded Systems](https://img.shields.io/badge/Embedded-Systems-6A5ACD?style=for-the-badge)

## 🧩 Hardware Components

| Component | Purpose |
|---|---|
| ESP32 WROOM-32 | Main microcontroller |
| BioAmp EXG Pill modules | EEG and EMG signal acquisition |
| L298N motor driver | DC motor control |
| DC motors and car chassis | Vehicle movement |
| Bluetooth-enabled control device | Wireless command interface |
| Battery and connecting wires | Power supply and interconnections |

*The component list describes the project setup; refer to the actual circuit and source code for the final wiring configuration.*

## ⚙️ How It Works

The system combines biosignal acquisition, control logic, wireless communication, and embedded motor control.

1. **Signal Acquisition:** EEG and EMG modules acquire electrical signals from the user.
2. **Signal Processing:** The control software interprets the available signal inputs according to the implemented logic.
3. **Command Generation:** The recognized control input is mapped to a vehicle movement command.
4. **Wireless Communication:** The control application communicates the command to the ESP32 over Bluetooth.
5. **Motor Control:** The ESP32 drives the L298N motor driver to control the car's motors.

### System Architecture


       EEG / EMG Sensors
               |
               v
       Bio-Signal Inputs
               |
               v
    Signal Interpretation
               |
               v
      Movement Commands
               |
               v
      Bluetooth Link
               |
               v
          ESP32
               |
               v
       L298N Motor Driver
               |
               v
        DC Motors
               |
               v
        Smart Car 🚗


## 💻 Software and Tools

- **Arduino IDE** — Embedded firmware development and uploading.
- **C/C++** — Microcontroller programming and control logic.
- **Bluetooth Serial Communication** — Wireless command transmission.
- **Git and GitHub** — Source code management and project documentation.

## 🚀 Getting Started

### Prerequisites

- Arduino IDE installed on your computer.
- ESP32 board support configured in Arduino IDE.
- ESP32 WROOM-32 development board.
- Required hardware components and a compatible power supply.
- The project source code from this repository.

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/Dark-004/BCI-ROBOT-CAR.git
```

**2. Open the project**

Open the downloaded source files in Arduino IDE.

**3. Configure the board**

Select the appropriate ESP32 board and communication port in Arduino IDE.

**4. Check the configuration**

Verify the sensor connections, motor driver pins, Bluetooth settings, and power connections against the project's source code and circuit.

**5. Upload and test**

Upload the firmware to the ESP32, power the car safely, and test the implemented control functions.

> Note: The exact firmware files, Bluetooth command format, and circuit configuration depend on the implementation in this repository.

## 🎯 Learning Outcomes

This project provided practical exposure to:

- Embedded C/C++ programming.
- Microcontroller-based hardware integration.
- Biosignal acquisition and control concepts.
- Bluetooth communication.
- DC motor control and motor driver interfacing.
- Hardware–software integration and debugging.

## 🔮 Future Improvements

- Integrate more robust real-time biosignal processing.
- Explore machine learning for improved signal classification.
- Add obstacle detection and collision avoidance.
- Develop a more intuitive mobile control interface.
- Introduce command validation and an emergency-stop mechanism.

## ⚠️ Safety Note

Neuro-Drive is an educational prototype, not a road-ready or medically validated vehicle-control system. Test the car in a controlled environment, use appropriate electrical protection, and provide a reliable manual stop mechanism.

## 👨‍💻 Author

**Martin Biju**

B.Tech Computer Science and Engineering |

- GitHub: [@Dark-004](https://github.com/Dark-004)
- LinkedIn: [Martin Biju](https://www.linkedin.com/in/martin-biju1220/)

---

⭐ **If you find this project interesting, consider starring the repository!**

*Exploring the intersection of biosignals, embedded systems, and intelligent human–machine interaction.*
