import random


def playrs_name() -> tuple[str, str]:
    name1 = input("Имя первого игрока: ")
    name2 = input("Имя второго игрока: ")
    return name1, name2


def random_user(name1: str, name2: str) -> tuple[str, str]:
    order = [name1, name2]
    random.shuffle(order)
    return order[0], order[1]
