Приложение "Зоотакси"

Суть приложения в том, чтобы отслеживать сотрудников такси в реальном времени. Это нужно для владельца организации.

Весь функционал приложения сделан с помощью Yandex Mapkit
- Посторение маршрутов (от точки до точки, от сотрудника до точки)
- Возможность ввести адрес

![Без имени-10](https://github.com/user-attachments/assets/14d087f4-0323-47b7-a63a-e010f0f94061)

Используется база данных Firebase Realtime Database для отслеживания всех сотрудников каждую секунду.

Для отправки местоположения используется Foreground Service, который работает в фоне даже если приложение свернуто или телефон заблокирован

У организатора специальный аккаунт со всеми функциями Yandex Mapkit, реализованный через Firebase Auth

![Без имени-11](https://github.com/user-attachments/assets/e676c844-b3e6-421b-b65b-40fa50faef68)

