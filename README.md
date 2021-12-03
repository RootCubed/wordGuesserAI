# [EProg HS 21] AI for task 4 of sheet 9

## The task
In this task, we had to write an AI for a word guessing game.
The game worked as follows: To begin, the AI gets a list of all possible words. Then, a word from the word list is randomly chosen. When the function `gibTipp` gets called, the AI must return a tip that helps it find the chosen word. The AI gets the clue via the function `bekommeHinweis`, which passes in one of the following answers:
- The word is {tip}
- The word doesn't contain {tip}
- The word contains {tip}
- The word begins with {tip}
- The word ends with {tip}
- The word begins and ends with {tip}

## The AI
We make use of the fact that always the same word list is used. This means we can use as much time as we want to generate decision tree that minimizes the average number of guesses. The package `wordGuesserGenerator` contains this generator. It creates instances of the class `Generator` which all run on separate threads. Each generator:
1. First picks the best out of 100 guess trees generated with a heuristic with default parameters (explained later)
2. Goes to either step 3 or 4.
3. Picks a random node from the guess tree and re-generates the subtree with the heuristic, but this time with randomized parameters. If the new guess tree is worse than the previous one, the previous one is restored. (Go to step 5)
4. Picks a random node from the guess tree and one of its child nodes. It swaps the two nodes and re-generates the subtree with the heuristic, again with randomized parameters. If the new guess tree is worse than the previous one, the previous one is restored.
5. Repeats from step 2 until the user ends the program.

## The heuristic
In order to find a good tip for a given word list, one can realize that any tip partitions the word list into 5 sets:
- words which don't contain the tip
- words which contain the tip, but it isn't at the beginning or end
- words which begin with the tip, but don't end with it
- words which end with the tip, but don't begin with it
- words which begin and end with the tip

We iterate through all 1-, 2-, and 3-letter long combinations of the letters a-z and ä, ö, ü and look at the distribution of the partition the generate. This gives an array of size 5 with values between 0 and 1 (0 means none of the words fall into this category, 1 mean all of the words fall into this category). The heuristic parameter is the partition ratio array it should most closely ressemble. The default is [0.2, 0.2, 0.2, 0.2, 0.2]. Note however that it is possible that even if we can find a distribution that exactly matches this parameter, it might not be the optimal guess because we are not looking at the guesses for each child node. To circumvent this problem, the AI uses randomized parameters as described above.

There are some additional parameters that the heuristic guesser takes in, these can be found in `HeuristicParams.java`.

## Results
Running this program with 14 threads for about half an hour yielded a guess tree that resulted in an average number of guesses of 7.220.