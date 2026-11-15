# Rover command line

Drive a rover with turn and move commands on a grid containing obstacles.

## Worked solution

Represent the heading as an index into north, east, south, west with displacement vectors (0,1), (1,0), (0,−1), (−1,0). A left turn subtracts one modulo four and a right turn adds one.

For a forward command, add the heading vector to the current position. If the destination is in the obstacle set, report a collision and preserve the position. Otherwise commit the destination. Rendering bounds only control the display.

Starting at (0,0) facing north with an obstacle at (0,1), forward leaves the rover still. Right then forward ends at (1,0) facing east.

## Reference implementation

Reconstructed Python example.

```python
def drive(commands, obstacles=(), start=(0, 0), heading=0):
    steps = [(0, 1), (1, 0), (0, -1), (-1, 0)]
    x, y = start
    blocked = set(obstacles)
    collisions = []
    for command in commands.upper().split():
        if command == "LEFT":
            heading = (heading - 1) % 4
        elif command == "RIGHT":
            heading = (heading + 1) % 4
        elif command == "FORWARD":
            dx, dy = steps[heading]
            target = (x + dx, y + dy)
            if target in blocked:
                collisions.append(target)
            else:
                x, y = target
        else:
            raise ValueError(command)
    return (x, y), heading, collisions


assert drive("forward right forward", [(0, 1)]) == ((1, 0), 1, [(0, 1)])
if __name__ == "__main__":
    print(drive(input("Enter LEFT, RIGHT or FORWARD commands: ")))
```
