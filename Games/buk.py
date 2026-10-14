from game_func import *
import random


def menu() -> None:
    print("\nМеню")
    print("1. Играть с компьютером")
    print("2. Играть вдвоем")
    print("3. Правила")
    print("4. Выход")


def read_four(player: str) -> list[int]:
    digits = []
    for i in range(4):
        digits.append(int(input(f"{player}, введите цифру: ")))
    return digits


def show_matches(secret: list[int], guess: list[int]) -> int:
    used_secret = [False, False, False, False]
    used_guess = [False, False, False, False]
    bulls = 0

    for i in range(4):
        if guess[i] == secret[i]:
            used_secret[i] = True
            used_guess[i] = True
            bulls += 1
            print(guess[i], "на своём месте")

    for a in range(4):
        if used_guess[a]:
            continue
        for i in range(4):
            if used_secret[i]:
                continue
            if guess[a] == secret[i]:
                used_secret[i] = True
                print(guess[a], "есть, но стоит не на своём месте")
                break

    return bulls


def try_guess(secret: list[int], player: str) -> None:
    lives = 5

    while lives > 0:
        guess = read_four(player)
        if show_matches(secret, guess) == 4:
            print(f"{player}, вы угадали")
            return

        lives -= 1
        print(f"Осталось попыток: {lives}")

    print("Попытки закончились")
    print("Число было:", secret)


def play_computer() -> None:
    computer_choice = []

    for i in range(4):
        computer_choice.append(random.randint(1, 10))

    try_guess(computer_choice, "Вы")


def play_two() -> None:
    user_name1, user_name2 = playrs_name()
    players1, players2 = random_user(name1=user_name1, name2=user_name2)

    print(f"{players1}, загадайте число")
    list_players1 = read_four(players1)
    print("\n" * 40)
    try_guess(list_players1, players2)

    print("\n\nТеперь меняемся ролями")
    print(f"{players2}, загадайте число")
    list_players2 = read_four(players2)
    print("\n" * 40)
    try_guess(list_players2, players1)


def game() -> None:
    while True:
        menu()
        try:
            choice = int(input("Ваш выбор: "))
        except ValueError:
            print("Введите лучше число!")
            continue

        if choice == 4:
            break

        elif choice == 1:
            play_computer()

        elif choice == 2:
            play_two()

        elif choice == 3:
            print("\nПравила игры")
            print("Если играете с компьютером, то он загадывает 4 цифры от 1 до 10 и вам нужно угадать, с 5 попыток")
            print("Если играете вдвоем, то случайным образом выбирается игрок который будет загадывать цифры от 1 до 10")
            print("и второй игрок, который должен угадать и у него будет 5 попыток, далее вы поменяетесь.")

        else:
            print("Введите число от 1 до 4")


if __name__ == "__main__":
    game()
