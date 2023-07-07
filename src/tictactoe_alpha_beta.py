import copy
import math

import numpy as np


# Function to check if a player has won the board
def is_winner(board, player_id):
    # Check if winner due to a filled row
    for row in range(3):
        winner = True

        for col in range(3):
            if board[row][col] != player_id:
                winner = False
                break

        if winner:
            return True

    # Check if winner due to a filled col
    for col in range(3):
        winner = True

        for row in range(3):
            if board[row][col] != player_id:
                winner = False
                break

        if winner:
            return True

    # Check if winner due to a diagonal
    winner = (board[0][0] == player_id and board[1][1] == player_id and board[2][2] == player_id) or (
            board[0][2] == player_id and board[1][1] == player_id and board[2][0] == player_id)
    if winner:
        return True

    return False


# Function to create successor board states
def create_successors(board_tree_node, player_id):
    board = board_tree_node.board
    for row in range(3):
        for col in range(3):
            if board[row][col] == 0:
                board_copy = copy.deepcopy(board)
                board_copy[row][col] = player_id
                board_tree_node.children.append(TreeNode(board_copy))


# Number of game tree nodes
nodes = 0


# Simple TreeNode class
class TreeNode():
    def __init__(self, board):
        self.board = board
        self.value = None
        self.children = []


# Maximizer
def max_value(board_tree_node, alpha, beta):
    global nodes

    nodes += 1
    value = -math.inf

    board = board_tree_node.board

    # Board is terminal state: if player_id = 1 wins, then return 1
    if is_winner(board, 1):
        board_tree_node.value = 1
        return 1

    # Board is terminal state: if player_id = 2 wins, then return -1
    if is_winner(board, 2):
        board_tree_node.value = -1
        return -1

    # Board is terminal state: if board is filled up and no player has won, return 0
    if np.all(board):
        board_tree_node.value = 0
        return 0

    # Create successor board states
    create_successors(board_tree_node, 1)

    # For each successor board state call min_value
    for a_board_tree_node in board_tree_node.children:
        v = min_value(a_board_tree_node, alpha, beta)
        if v > value:
            value = v
            alpha = max(alpha, value)
        if v >= beta:
            board_tree_node.value = value
            return value

    board_tree_node.value = value
    return value


# Minimizer
def min_value(board_tree_node, alpha, beta):
    global nodes

    nodes += 1
    value = math.inf

    board = board_tree_node.board

    # Board is terminal state: if player_id = 1 wins, then return 1
    if is_winner(board, 1):
        board_tree_node.value = 1
        return 1

    # Board is terminal state: if player_id = 2 wins, then return -1
    if is_winner(board, 2):
        board_tree_node.value = -1
        return -1

    # Board is terminal state: if board is filled up and no player has won, return 0
    if np.all(board):
        board_tree_node.value = 0
        return 0

    # Create successor board states
    create_successors(board_tree_node, 2)

    # For each successor board state call max_value
    for a_board_tree_node in board_tree_node.children:
        v = max_value(a_board_tree_node, alpha, beta)

        if v < value:
            value = v
            beta = min(beta, value)
        if v <= alpha:
            board_tree_node.value = value
            return value

    board_tree_node.value = value
    return value


# Prints board sequence
# Assuming that player_1 is going first
def print_board_sequence(node):
    player_1 = True

    while (node):
        print(np.matrix(node.board))
        if not node.children:
            break
        if player_1:
            node.children.sort(key=lambda x: x.value, reverse=True)
            node = node.children[0]
            player_1 = False
        else:
            node.children.sort(key=lambda x: x.value)
            node = node.children[0]
            player_1 = True


# Set a start board state
start_board = [[1, 2, 0], [0, 1, 0], [0, 0, 2]]

# Create start tree node
start_board_node = TreeNode(start_board)

# Print the max value of the start tree
# (Assuming here that the next turn is of player_1)
print(max_value(start_board_node, -math.inf, math.inf))

# Print number of tree nodes
print(f'Number of TreeNodes: {nodes}')

# Print the sequence of board states
print_board_sequence(start_board_node)
