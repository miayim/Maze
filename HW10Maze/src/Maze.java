import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.NoSuchElementException;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.Random;
import java.util.function.Predicate;


// Kruskal Maze Generator and Solver 
// --- HOW TO USE ---
// once an automatic search begins, you cannot stop/switch searches until finished  
// - press 'd' to start automatic depth first search
// - press 'b' to start automatic breadth first search
// - if a search hasn't started, press 'm' to move manually. 
// - After 'm' press, use arrow keys as normal to move

// TODO testing
// TODO check private and public
// TODO scale cells


// To represent a Cell in the maze with an x and y position and a size
class Cell {
  private final int x;
  private final int y;
  private final int size;

  Cell(int x, int y) {
    this.x = x;
    this.y = y;
    this.size = 15;
  }

  // overridden hashCode for Cell
  public int hashCode() {
    return (this.x * 1000) + this.y;
  }

  // overridden equality for Cell
  public boolean equals(Object other) {
    if (!(other instanceof Cell)) {
      return false;
    }
    Cell that = (Cell) other;
    return this.x == that.x && this.y == that.y;
  }

  // returns the string direction that the given cell is from this cell
  String direction(Cell to) {
    if (to.x < this.x) {
      return "west";
    }
    else if (to.y < this.y) {
      return "north";
    }
    else if (to.y > this.y) {
      return "south";
    }
    else {
      return "east";
    }
  }

  public void drawMyWall(WorldScene w, String dir) {
    if (dir.equals("north")) {
      w.placeImageXY(new RectangleImage(size, 1, OutlineMode.SOLID, Color.black),
          (this.x * size) + size / 2, this.y * size);
    }
    else if (dir.equals("south")) {
      w.placeImageXY(new RectangleImage(size, 1, OutlineMode.SOLID, Color.black),
          (this.x * size) + size / 2, (this.y * size) + size);
    }
    else if (dir.equals("west")) {
      w.placeImageXY(new RectangleImage(1, size, OutlineMode.SOLID, Color.black), this.x * size,
          (this.y * size) + size / 2);
    }
    else if (dir.equals("east")) {
      w.placeImageXY(new RectangleImage(1, size, OutlineMode.SOLID, Color.black),
          (this.x * size) + size, (this.y * size) + size / 2);
    }
  }

  //renders this cell as the given color
  void drawCell(WorldScene w, Color c) {
    w.placeImageXY(
        new RectangleImage(this.size, this.size, OutlineMode.SOLID, c),
        (this.x * this.size) + (this.size / 2), (this.y * this.size) + this.size / 2);
  }

  // renders the start cell green and the end cell purple
  void drawStartAndEnd(WorldScene w, int width, int height) {
    WorldImage start = new RectangleImage(this.size, this.size, OutlineMode.SOLID,
        new Color(0, 153, 0));
    WorldImage end = new RectangleImage(this.size, this.size, OutlineMode.SOLID,
        new Color(102, 0, 153));

    w.placeImageXY(start, this.size / 2, this.size / 2);
    w.placeImageXY(end, width - this.size / 2, height - this.size / 2);
  }

  // checks if moving from this cell to the desired cell (based on given dx, dy)
  // is valid, as in there exists a wall in the minimum spanning tree connecting this cell to the
  // desired cell.
  // returns either this cell or the desired cell based on the validity
  Cell nextCell(ArrayList<Wall> tree, int dx, int dy, int width, int height) {
    Cell next = new Cell(this.x + dx, this.y + dy);
    if (!withinBounds(width, height)) {
      return this;
    }
    for (Wall w : tree) {
      if (w.containsCell(this) && w.containsCell(next)) {
        return next;
      }
    }
    return this;
  }

  // returns true if this cell is within the given boundaries
  boolean withinBounds(int width, int height) {
    return (this.x > 0 || this.y > 0 || this.x < width || this.y < height);
  }

  //compares two cells based on x and y posn for sorting
  // ex: (0, 0).compare(0,1) = - 1
  int cellCompare(Cell other) {
    if (this.y < other.y) {
      return -1;
    }
    else if (this.y > other.y) {
      return 1;
    }
    else if (this.x < other.x) {
      return -1;
    }
    else if (this.x > other.x) {
      return 1;
    }
    else {
      return 0;
    }
  }

  boolean isBeginning() {
    return this.x == 0 && this.y == 0;
  }

  boolean isEnding(int width, int height) {
    return this.x == width - 1 && this.y == height - 1;
  }
}

// To represent an edge in the graph that represents the maze
class Wall {

  private final Cell from;
  private final Cell to;
  private final int weight;

  Wall(Cell from, Cell to, int weight) {
    this.from = from;
    this.to = to;
    this.weight = weight;
  }

  // overridden equality for wall
  public boolean equals(Object other) {
    if (!(other instanceof Wall)) {
      return false;
    }
    else {
      Wall that = (Wall) other;
      return (this.from.equals(that.from) && this.to.equals(that.to))
          || (this.from.equals(that.to) && this.to.equals(that.from));
    }
  }

  // overridden hashCode for wall
  public int hashCode() {
    return (this.from.hashCode() * 10000) + this.to.hashCode();
  }

  // returns true if this wall is connecting the given cell to another cell
  boolean containsCell(Cell c) {
    return this.from.equals(c) || this.to.equals(c);
  }

  // return the other point that this wall is connecting the given cell to
  Cell otherPoint(Cell c) {
    if (this.from.equals(c)) {
      return this.to;
    }
    else if (this.to.equals(c)) {
      return this.from;
    }
    else {
      throw new NoSuchElementException("the given cell is not in this wall");
    }
  }

  // returns the difference between this wall's weight and the given wall's weight
  int weightDifference(Wall other) {
    return this.weight - other.weight;
  }

  // checks if adding this edge would create a cycle
  boolean alreadyConnected(HashMap<Cell, Cell> reps) {
    Cell fromStart = this.find(reps, this.from); // find X
    Cell toStart = this.find(reps, this.to); // find Y

    // from and to have the same representative cell
    if (fromStart.equals(toStart)) {
      return true;
    }
    else {
      return false;
    }
  }

  // finds the representative node for the target cell
  Cell find(HashMap<Cell, Cell> reps, Cell target) {
    if (reps.get(target).equals(target)) {
      return target;
    }
    else {
      return find(reps, reps.get(target));
    }
  }

  // unions the representatives of the cell of the given wall
  void union(HashMap<Cell, Cell> reps, Wall source) {
    Cell repFrom = this.find(reps, source.from);
    Cell repTo = this.find(reps, source.to);
    reps.put(repTo, repFrom);
  }

  // adds this wall's to and from cells to the allCells ArrayList
  void addWallCells(ArrayList<Cell> cells) {
    this.addUniqueCells(cells, this.to);
    this.addUniqueCells(cells, this.from);
  }

  // adds the given cell if it is unique in the ArrayList
  void addUniqueCells(ArrayList<Cell> cells, Cell curr) {
    boolean shouldAdd = true;
    for (Cell c : cells) {
      if (curr.equals(c)) {
        shouldAdd = false;
      }
    }
    if (shouldAdd) {
      cells.add(curr);
    }
  }

  // returns the string representation of the direction this wall going
  String direction() {
    return this.from.direction(this.to);
  }

  //renders this wall
  public void drawWall(WorldScene w) {
    String dir = this.direction();
    this.from.drawMyWall(w, dir);
  }
}

// To represent a graph made up of walls and cells that creates a solvable maze with
// no cycles
class Maze extends World {
  private final int width;
  private final int height;
  private final Random rand;
  private ArrayList<Wall> allWalls;
  private ArrayList<Cell> allCells;
  private HashMap<Cell, Cell> representatives;
  private ArrayList<Wall> edgesInTree;
  private Cell start;
  private Cell end;
  private ICollection<Cell> worklist;
  private ArrayList<Cell> alreadySeen;
  private HashMap<Cell, Cell> cameFromEdge;
  private ArrayList<Cell> finalPath;
  private boolean solved;
  private boolean worldEnd;
  private boolean shouldStart;
  private Cell player;
  private boolean manual;

  Maze(int width, int height) {
    this.width = width;
    this.height = height;
    this.rand = new Random();
    this.solved = false;
    this.makeMaze();
    this.worldEnd = false;
    this.shouldStart = false;
    this.manual = false;
  }

  Maze(int width, int height, Random r) {
    this.width = width;
    this.height = height;
    this.rand = new Random(this.width * this.height);
    this.solved = false;
    this.makeMaze();
    this.worldEnd = false;
    this.shouldStart = false;
    this.manual = false;
  }

  Maze(int width, int height, ArrayList<Wall> allWalls, ArrayList<Cell> allCells, ICollection<Cell> worklist, ArrayList<Cell> alreadySeen, 
      HashMap<Cell, Cell> cameFromEdge) {
    this.width = width;
    this.height = height;
    this.rand = new Random(this.width * this.height);
    this.solved = false;
    this.worldEnd = false;
    this.shouldStart = false;
    this.manual = false;
    this.allWalls = allWalls;
    this.allCells = allCells;
    this.representatives = this.createMap(allCells);
    this.edgesInTree = this.kruskal(allWalls);
    this.start = allCells.get(0);
    this.end = this.findEnd(allCells);
    this.finalPath = new ArrayList<Cell>();
    this.worklist = worklist;
    //this.worklist.add(this.start);
    this.alreadySeen = alreadySeen;
    this.cameFromEdge = cameFromEdge;
    this.player = this.findStart(allCells);
  }


  // TODO ASK IF WE NEED TO TEST THIS
  // initializes the maze's representations cells and walls 
  void makeMaze() {
    this.allWalls = this.createEdges();
    this.allCells = this.getAllCells(allWalls);
    this.representatives = this.createMap(allCells);
    this.edgesInTree = this.kruskal(allWalls);
    this.start = this.findStart(allCells);
    this.end = this.findEnd(allCells);
    this.finalPath = new ArrayList<>();
    this.worklist = new Queue<Cell>();
    this.worklist.add(this.start);
    this.alreadySeen = new ArrayList<Cell>();
    this.cameFromEdge = new HashMap<Cell, Cell>();
    this.player = this.findStart(allCells);
  }

  // creates an initial hash map where each Cell is linked to itself
  HashMap<Cell, Cell> createMap(ArrayList<Cell> cells) {
    HashMap<Cell, Cell> representatives = new HashMap<>();
    for (Cell c : cells) {
      representatives.put(c, c);
    }
    return representatives;
  }

  // Finds the initial starting cell located at (0, 0)
  Cell findStart(ArrayList<Cell> allCells) {
    Cell potentialCurr = allCells.get(0);
    for (Cell c : allCells) {
      if (c.isBeginning()) {
        potentialCurr = c;
      }
    }
    return potentialCurr;
  }

  // Finds the final ending cell located at (width, height)
  // in the bottom right corner
  Cell findEnd(ArrayList<Cell> allCells) {
    Cell potentialCurr = allCells.get(0);

    for (Cell c : allCells) {
      if (c.isEnding(this.width, this.height)) {
        potentialCurr = c;
      }
    }
    return potentialCurr;
  }

  // creates a list of all edges with randomized weights connecting two
  // neighboring cells representing a grid
  ArrayList<Wall> createEdges() {
    int area = this.width * this.height;

    ArrayList<Wall> edges = new ArrayList<Wall>();

    for (int y = 0; y < this.height; y += 1) {
      for (int x = 0; x < this.width; x += 1) {
        Cell curr = new Cell(x, y);
        // add wall only if its valid bounds
        if (y > 0) {
          Wall north = new Wall(curr, new Cell(x, y - 1), rand.nextInt(area));
          addUniqueEdges(edges, north);
        }
        if (y < this.height - 1) {
          Wall south = new Wall(curr, new Cell(x, y + 1), rand.nextInt(area));
          addUniqueEdges(edges, south);
        }
        if (x < this.width - 1) {
          Wall east = new Wall(curr, new Cell(x + 1, y), rand.nextInt(area));
          addUniqueEdges(edges, east);
        }
        if (x > 0) {
          Wall west = new Wall(curr, new Cell(x - 1, y), rand.nextInt(area));
          addUniqueEdges(edges, west);
        }
      }
    }
    return edges;
  }

  // adds the given wall to the given list of walls if it is not already present
  // (avoids two directed edges between two nodes in favor of one "undirected"
  // edge)
  void addUniqueEdges(ArrayList<Wall> walls, Wall curr) {
    boolean shouldAdd = true;
    for (Wall w : walls) {
      if (curr.equals(w)) {
        shouldAdd = false;
      }
    }
    if (shouldAdd) {
      walls.add(curr);
    }
  }

  // creates a minimum spanning tree using Kruskal's algorithm
  ArrayList<Wall> kruskal(ArrayList<Wall> walls) {
    ArrayList<Wall> worklist = new ArrayList<>();
    for (int i = 0; i < walls.size(); i += 1) {
      worklist.add(walls.get(i));
    }
    // sort the work list by lowest to highest weights
    Collections.sort(worklist, new Comparator<Wall>() {
      public int compare(Wall w1, Wall w2) {
        if (w1.weightDifference(w2) > 0) {
          return 1;
        }
        else if (w1.weightDifference(w2) < 0) {
          return -1;
        }
        else {
          return 0;
        }
      }
    });

    ArrayList<Wall> mst = new ArrayList<>();

    // while there is more than one tree
    while (mst.size() < this.allCells.size() - 1) {

      Wall curr = worklist.get(0);

      // get the lowest edge and check if its already connected
      if (curr.alreadyConnected(this.representatives)) {
        worklist.remove(0); // edge is connected, discard it
      }
      else {
        // non cyclic path, so add it to the mst
        curr.union(this.representatives, curr);
        mst.add(worklist.remove(0));
      }
    }
    return mst;
  }

  // collects all of the Cells from a list of Walls in an ArrayList
  ArrayList<Cell> getAllCells(ArrayList<Wall> walls) {
    ArrayList<Cell> cells = new ArrayList<>();
    for (Wall w : walls) {
      w.addWallCells(cells);
    }
    // sorts this list of cells from (0,0) (0,1) (0,2) ... (width, height)
    Collections.sort(cells, new Comparator<Cell>() {
      public int compare(Cell c1, Cell c2) {
        return c1.cellCompare(c2);
      }
    }
        );
    return cells;
  }

  // returns true if the next element is the end of the maze (the maze is solved)
  // otherwise, continues traversal for the next element in the search (either breadth first or
  // depth first search)
  boolean nextElem() {
    if (!this.worklist.isEmpty()) {
      Cell curr = this.worklist.remove();
      if (alreadySeen.contains(curr)) {
        // do nothing: we've already seen this one
      }
      else if (curr.equals(this.end)) {
        this.solved = true;
        this.solution();
        return true;
      }
      else {
        for (Wall w : this.edgesInTree) {
          if (w.containsCell(curr)) {
            this.worklist.add(w.otherPoint(curr));
            this.cameFromEdge.putIfAbsent(w.otherPoint(curr), curr);
          }
        }
        this.alreadySeen.add(curr);
      }
    }
    return false;
  }

  // backtracks through the hash map cameFromEdge, which accumulated the path from
  // start to finish, and populates the solution arraylist with the final solution path
  void solution() {
    Cell curr = this.end;

    while (!curr.equals(start)) {
      this.finalPath.add(curr);
      curr = this.cameFromEdge.get(curr);
    }
    this.finalPath.add(curr);
    this.solved = true;
    this.worldEnd = true;
  }

  // moves the player, depending on the validity of the move and the given
  // dy and dx
  void movePlayer(int dy, int dx) {
    Cell move = this.player.nextCell(this.edgesInTree, dx, dy, this.width, this.height);
    this.alreadySeen.add(this.player);
    this.player = move;

    if(move.equals(this.end)) {
      this.solved = true;
      this.worldEnd = true;
    }
  }

  // renders the maze as a scene
  public WorldScene makeScene() {

    ArrayList<Wall> mazeWalls = new ArrayList<Wall>();
    for (Wall w : this.allWalls) {
      if (!(this.edgesInTree.contains(w))) {
        mazeWalls.add(w);
      }
    }

    WorldScene w = new WorldScene(this.width * 15, this.height * 15);

    for (int i = 0; i < this.allCells.size(); i += 1) {
      this.allCells.get(i).drawCell(w, new Color(204, 204, 204));
    }

    for (int i = 0; i < this.alreadySeen.size(); i += 1) {
      this.alreadySeen.get(i).drawCell(w, new Color(51, 204, 255));
    }

    if (solved) {
      for (int i = 0; i < this.finalPath.size(); i += 1) {
        this.finalPath.get(i).drawCell(w, new Color(0, 0, 220));
      }
    }

    for (int i = 0; i < mazeWalls.size(); i += 1) {
      mazeWalls.get(i).drawWall(w);
    }

    this.allCells.get(0).drawStartAndEnd(w, this.width * 15, this.height * 15);

    this.player.drawCell(w, new Color(255, 255, 153));

    return w;
  }

  // scene showing that the game ends
  public WorldScene lastScene(String msg) {

    ArrayList<Wall> mazeWalls = new ArrayList<Wall>();
    for (Wall w : this.allWalls) {
      if (!(this.edgesInTree.contains(w))) {
        mazeWalls.add(w);
      }
    }
    WorldScene w = new WorldScene(this.width * 15, this.height * 15);

    for (int i = 0; i < this.allCells.size(); i += 1) {
      this.allCells.get(i).drawCell(w, new Color(204, 204, 204));
    }
    for (int i = 0; i < this.alreadySeen.size(); i += 1) {
      this.alreadySeen.get(i).drawCell(w, new Color(51, 204, 255));
    }
    if (solved) {
      for (int i = 0; i < this.finalPath.size(); i += 1) {
        this.finalPath.get(i).drawCell(w, new Color(0, 0, 220));
      }
    }
    for (int i = 0; i < mazeWalls.size(); i += 1) {
      mazeWalls.get(i).drawWall(w);
    }
    this.allCells.get(0).drawStartAndEnd(w, this.width * 15, this.height * 15);
    w.placeImageXY(new TextImage("MAZE SOLVED", 24, Color.RED), (this.width * 15) / 2, 
        (this.height * 15) / 2);

    return w;
  }

  // continues the next step in the breadth first or depth first search for a path
  // on each valid tick
  public void onTick() {
    if (this.worldEnd) {
      this.endOfWorld("Maze Completed");
    }
    else {
      if (this.shouldStart) {
        this.nextElem();
      }
    }
  }

  // starts either breadth first, depth first, or manual search for a path through the maze
  // based on the given key press
  public void onKeyEvent(String s) {
    if (s.equals("b") && !this.shouldStart) {
      this.worklist = new Queue<Cell>();
      this.worklist.add(this.start);
      this.shouldStart = true;
    }
    else if (s.equals("d") && !this.shouldStart) {
      this.worklist = new Stack<Cell>();
      this.worklist.add(this.start);
      this.shouldStart = true;
    }

    else if (s.equals("m") && !this.shouldStart) {
      this.manual = true;
    }

    else if (s.equals("r")) {
      this.makeMaze();
      this.shouldStart = false;
    }

    // moving up means the player's y position is one less than it was before and
    // the x position does not change.
    // ex: if the player was in cell (2, 2), then after the move, the player is in
    // cell (1, 2)
    // therefore, (dy, dx) = (-1, 0)
    else if (s.equals("up") && this.manual) {
      this.movePlayer(-1, 0);
    }
    // moving down means the player's y position is one more than it was before and
    // the x position does not change.
    // ex: if the player was in cell (2, 2), then after the move, the player is in
    // cell (3, 2)
    // therefore, (dy, dx) = (1, 0)
    else if (s.equals("down") && this.manual) {
      this.movePlayer(1, 0);
    }
    // moving left means the player's y position does not change and the x position
    // is one less than it was before.
    // ex: if the player was in cell (2, 2), then after the move, the player is in
    // cell (2, 1)
    // therefore, (dy, dx) = (0, -1)
    else if (s.equals("left") && this.manual) {
      this.movePlayer(0, -1);
    }
    // moving left means the player's y position does not change and the x position
    // is one more than it was before.
    // ex: if the player was in cell (2, 2), then after the move, the player is in
    // cell (2, 3)
    // therefore, (dy, dx) = (0, 1)
    else if (s.equals("right") && this.manual) {
      this.movePlayer(0, 1);
    }
  }
}

// Represents a mutable collection of items
interface ICollection<T> {
  // Is this collection empty?
  boolean isEmpty();

  // EFFECT: adds the item to the collection
  void add(T item);

  // Returns the first item of the collection
  // EFFECT: removes that first item
  T remove();
}

// Represents an element of the deque which is either a sentinel or a node
abstract class ANode<T> {

  ANode<T> next;
  ANode<T> prev;

  ANode() {
    this.next = this;
    this.prev = this;
  }

  // counts the number of nodes ANode has including itself
  int sizeHelp() {
    return 1 + this.next.sizeHelp();
  }

  // returns this ANode's T data at the given index
  abstract T removeHelp(int index);

  // EFFECT: adds the given element at the given index by adjusting the next and
  // previous around this ANode
  void addHelp(T elem, int index) {
    if (index == 0) {
      ANode<T> oldNext = this.next;
      ANode<T> newElem = new Node<T>(elem, oldNext, this);
      this.next = newElem;
      oldNext.prev = newElem;
    }
    else {
      this.next.addHelp(elem, index - 1);
    }
  }

  // EFFECT: updates this ANode's previous with the other's previous
  void updatePrev(ANode<T> other) {
    this.prev = other.prev;
  }

  // EFFECT: updates this ANode's next with the other's next
  void updateNext(ANode<T> other) {
    this.next = other.next;
  }

  // determines whether this deque contains the given element
  // by iterating through the deque
  boolean contains(T that) {
    return this.next.containsHelp(that);
  }

  // helps determine whether this deque contains the given element
  // by iterating through the deque
  abstract boolean containsHelp(T that);
}

// To represent a node in a deque which has data, a next, and a previous
class Node<T> extends ANode<T> {
  T data;

  Node(T data) {
    super();
    this.data = data;
  }

  Node(T data, ANode<T> next, ANode<T> prev) {
    super();
    this.data = data;
    if (next == null) {
      throw new IllegalArgumentException("Cannot have a null Node");
    }
    else {
      if (prev == null) {
        throw new IllegalArgumentException("Cannot have a null Node");
      }
      else {
        this.prev = prev;
      }
      this.next = next;
    }
    next.prev = this;
    prev.next = this;
  }

  // returns this Node's T data at the given index
  T removeHelp(int index) {
    if (index == 0) {
      this.next.updatePrev(this);
      this.prev.updateNext(this);
      return this.data;
    }
    else {
      return this.next.removeHelp(index - 1);
    }
  }

  // helps determine if this element is in the deque by checking
  // if it is in this node and if it isn't, passing it onto the next neighbor to check
  boolean containsHelp(T that) {
    if (this.data.equals(that)) {
      return true;
    }
    else {
      return this.next.containsHelp(that);
    }
  }

}

// To represent a header of the deque with a next and previous from which we can access all nodes
class Sentinel<T> extends ANode<T> {

  Sentinel(ANode<T> next, ANode<T> prev) {
    super();
    this.next = next;
    this.prev = prev;
  }

  Sentinel() {
    super();
    next = this;
    prev = this;
  }

  // overridden from abstract class:
  // counts the number of Nodes this Sentinel "is" (none)
  int sizeHelp() {
    return 0;
  }

  // counts the number of Nodes this Sentinel has not including the header (so
  // this)
  int sizeHeader() {
    return this.next.sizeHelp();
  }

  // continues removeHelp to this Sentinel's next
  // since this Sentinel has no data T to return
  T removeHelp(int index) {
    return this.next.removeHelp(index);
  }

  // returns false because if the sentinel is reached in the helper
  // method it is the second time we have seen the sentinel and therefore
  // the given element is not in the deque
  boolean containsHelp(T that) {
    return false;
  }
}

// To represent a deque with a sentinel from which we can access all of its nodes
class Deque<T> {
  Sentinel<T> header;

  Deque(Sentinel<T> header) {
    this.header = header;
  }

  Deque() {
    this.header = new Sentinel<T>();
  }

  // counts the number of Nodes in this Deque, not including the header node
  int size() {
    return this.header.sizeHeader();
  }

  // inserts the given element at the front of this Deque
  void addAtHead(T elem) {
    new Node<T>(elem, this.header.next, this.header);
  }

  // inserts the given element at the back of this Deque
  void addAtTail(T elem) {
    new Node<T>(elem, this.header, this.header.prev);
  }

  // Removes the first node from this Deque
  T removeFromHead() {
    if (this.size() == 0) {
      throw new RuntimeException("Cannot remove an element from an empty list");
    }
    else {
      return this.header.removeHelp(0);
    }
  }

  // determines if this deque contains the given element 
  boolean contains(T that) {
    if (this.size() == 0) {
      return false;
    }
    else {
      return this.header.contains(that);
    }
  }
}

// To represent a collection in which items are removed and added at the head
// (first in first out)
class Stack<T> implements ICollection<T> {
  Deque<T> contents;

  Stack() {
    this.contents = new Deque<T>();
  }

  // determines if this stack is empty
  public boolean isEmpty() {
    return this.contents.size() == 0;
  }

  // removes the node at the head
  public T remove() {
    return this.contents.removeFromHead();
  }

  // adds the given element at the head of the contents
  public void add(T item) {
    this.contents.addAtHead(item);
  }
}

// To represent a collection in which items are removed at the head but
// added at the tail (first in last out)
class Queue<T> implements ICollection<T> {
  Deque<T> contents;

  Queue() {
    this.contents = new Deque<T>();
  }

  // determines if this queue is empty
  public boolean isEmpty() {
    return this.contents.size() == 0;
  }

  // removes the node at the head of the contents
  public T remove() {
    return this.contents.removeFromHead();
  }

  // adds the given element at the tail of the contents
  public void add(T item) {
    this.contents.addAtTail(item);
  }
}

// To represent examples and tests on Maze
class ExamplesMaze {



  Cell a = new Cell(0, 0); // start- top left
  Cell b = new Cell(1, 0);
  Cell c = new Cell(2, 0);
  Cell d = new Cell(0, 1);
  Cell e = new Cell(1, 1);
  Cell f = new Cell(2, 1);
  Cell g = new Cell(0, 2);
  Cell h = new Cell(1, 2);
  Cell i = new Cell(2, 2); // end- bottom right

  Wall ab = new Wall(a, b, 8);
  Wall ad = new Wall(a, d, 5);
  Wall bc = new Wall(b, c, 4);
  Wall be = new Wall(b, e, 0);
  Wall ba = new Wall(b, a, 5);
  Wall cb = new Wall(c, b, 2);
  Wall cf = new Wall(c, f, 1);
  Wall da = new Wall(d, a, 6);
  Wall de = new Wall(d, e, 1);
  Wall dg = new Wall(d, g, 2);
  Wall eb = new Wall(e, b, 1);
  Wall ef = new Wall(e, f, 0);
  Wall eh = new Wall(e, h, 5);
  Wall ed = new Wall(e, d, 5);
  Wall fc = new Wall(f, c, 6);
  Wall fe = new Wall(f, e, 5);
  Wall fi = new Wall(f, i, 2);
  Wall gd = new Wall(g, d, 7);
  Wall gh = new Wall(g, h, 7);
  Wall hg = new Wall(h, g, 4);
  Wall he = new Wall(h, e, 3);
  Wall hi = new Wall(h, i, 5);
  Wall ih = new Wall(i, h, 3);
  Wall iF = new Wall(i, f, 2);

  Cell w = new Cell(0, 0);
  Cell x = new Cell(1, 0);
  Cell y = new Cell(0, 1);
  Cell z = new Cell(1, 1);

  Wall wx = new Wall(w, x, 3);
  Wall wy = new Wall(w, y, 2);
  Wall xw = new Wall(x, w, 4);
  Wall xz = new Wall(x, z, 1);
  Wall zx = new Wall(z, x, 5);
  Wall yw = new Wall(y, w, 2);
  Wall yz = new Wall(y, z, 2);
  Wall zy = new Wall(z, y, 2);



  ArrayList<Wall> walls = new ArrayList<Wall>();
  ArrayList<Cell> cells = new ArrayList<Cell>();
  ArrayList<Wall> walls2 = new ArrayList<Wall>();
  ArrayList<Cell> cells2 = new ArrayList<Cell>();
  ICollection<Cell> worklist = new Queue<Cell>();
  ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
  HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
  ICollection<Cell> worklist2 = new Stack<Cell>();
  

  ArrayList<Cell> alreadySeen2 = new ArrayList<Cell>();
  

  HashMap<Cell, Cell> cameFromEdge2 = new HashMap<Cell, Cell>();
  




  void initTemp() {

    walls.clear();
    walls.add(ad);
    walls.add(ab);
    walls.add(be);
    walls.add(bc);
    walls.add(cf);
    walls.add(dg);
    walls.add(de);
    walls.add(eh);
    walls.add(ef);
    walls.add(fi);
    walls.add(gh);
    walls.add(hi); 

    cells.clear();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);
    
    walls2.clear();
    walls2.add(wx);
    walls2.add(wy);
    walls2.add(xw);
    walls2.add(xz);
    walls2.add(zx);
    walls2.add(zy);
    
    cells2.clear();
    cells2.add(w);
    cells2.add(x);
    cells2.add(y);
    cells2.add(z);
    
    worklist.add(i);
    
    alreadySeen.clear();
    alreadySeen.add(a);
    alreadySeen.add(b);
    alreadySeen.add(c);
    alreadySeen.add(d);
    alreadySeen.add(e);
    alreadySeen.add(f);
    alreadySeen.add(g);
    alreadySeen.add(h);

    cameFromEdge.put(a, a);
    cameFromEdge.put(d, a);
    cameFromEdge.put(e, d);
    cameFromEdge.put(f, e);
    cameFromEdge.put(i, f);
    
    worklist2.add(z);
    
    alreadySeen2.clear();
    alreadySeen2.add(w);
    alreadySeen2.add(x);
    alreadySeen2.add(y);
    
    cameFromEdge2.put(w, w);
    cameFromEdge2.put(x, w);
    cameFromEdge2.put(z, x);

  }


  void testBigBang(Tester t) {
    
    this.initTemp();
    
    Maze maze = new Maze(3, 3, walls, cells, worklist, alreadySeen, cameFromEdge);
    Maze maze2 = new Maze(2, 2, walls2, cells2, worklist2, alreadySeen2, cameFromEdge2);
    
    maze.solution();
    maze2.solution();
    
    Maze s = maze;
    int worldWidth = 1000;
    int worldHeight = 1000;
    double tickRate = .0001;
    s.bigBang(worldWidth, worldHeight, tickRate);
  }






  // ------------------------- CELL METHODS ------------------------------ //

  public boolean testCellHashCode(Tester t) {
    Cell a = new Cell(0, 0);
    Cell alsoA = new Cell(0, 0);
    Cell b = new Cell(0, 1);
    Cell x = new Cell(1, 0);

    return t.checkExpect(a.hashCode(), 0) && t.checkExpect(alsoA.hashCode(), 0)
        && t.checkExpect(b.hashCode(), 1) && t.checkExpect(x.hashCode(), 1000);
  }

  public boolean testCellEquality(Tester t) {
    Cell a = new Cell(0, 0);
    Cell alsoA = new Cell(0, 0);
    Cell b = new Cell(0, 1);
    Cell x = new Cell(1, 0);
    Cell alsoX = new Cell(1, 0);

    return t.checkExpect(a.equals(alsoA), true) && t.checkExpect(a.equals(b), false)
        && t.checkExpect(alsoA.equals(b), false) && t.checkExpect(b.equals(x), false)
        && t.checkExpect(x.equals(alsoX), true);
  }

  public boolean testCellDirection(Tester t) {
    // this method is only for direct neighbors
    Cell a = new Cell(0, 0);
    Cell c = new Cell(1, 0);

    Cell b = new Cell(0, 1);
    Cell d = new Cell(1, 1);

    return t.checkExpect(a.direction(b), "south") && t.checkExpect(b.direction(a), "north")
        && t.checkExpect(d.direction(b), "west") && t.checkExpect(a.direction(c), "east")
        && t.checkExpect(d.direction(c), "north");
  }

  public boolean testCellDrawing(Tester t) {
    Cell a = new Cell(0, 0);
    Cell c = new Cell(1, 0);
    Cell b = new Cell(0, 1);
    Cell d = new Cell(1, 1);

    WorldScene w = new WorldScene(15, 15);
    Color gray = new Color(204, 204, 204);
    Color lightBlue = new Color(51, 204, 255);
    Color darkBlue = new Color(0, 0, 220);

    a.drawCell(w, gray);
    b.drawCell(w, gray);
    c.drawCell(w, darkBlue);
    d.drawCell(w, lightBlue);

    WorldScene result = new WorldScene(15, 15);
    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, gray), 15 / 2, 15 / 2);
    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, gray), 15 / 2, 15 + 15 / 2);
    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, darkBlue), 15 + 15 / 2,
        15 / 2);
    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, lightBlue), 15 + 15 / 2,
        15 + 15 / 2);
    return t.checkExpect(w, result);
  }

  boolean testCellDrawWall(Tester t) {
    Cell a = new Cell(0, 0);
    Cell c = new Cell(1, 0);
    Cell b = new Cell(0, 1);
    Cell d = new Cell(1, 1);

    String abDir = a.direction(b);
    String dbDir = d.direction(b);
    String dcDir = d.direction(c);

    WorldScene w = new WorldScene(15, 15);
    WorldScene result = new WorldScene(15, 15);

    a.drawMyWall(w, abDir);
    result.placeImageXY(new RectangleImage(15, 1, OutlineMode.SOLID, Color.black), 15 / 2, 15);
    d.drawMyWall(w, dbDir);
    result.placeImageXY(new RectangleImage(1, 15, OutlineMode.SOLID, Color.black), 15, 15 + 15 / 2);
    d.drawMyWall(w, dcDir);
    result.placeImageXY(new RectangleImage(15, 1, OutlineMode.SOLID, Color.black), 15 + 15 / 2, 15);

    return t.checkExpect(w, result);
  }

  boolean testDrawStartAndEnd(Tester t) {
    WorldScene w = new WorldScene(15, 15);
    WorldScene result = new WorldScene(15, 15);

    Cell start = new Cell(0, 0);

    start.drawStartAndEnd(w, 10, 10);

    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, new Color(0, 153, 0)), 15 / 2,
        15 / 2);

    result.placeImageXY(new RectangleImage(15, 15, OutlineMode.SOLID, new Color(102, 0, 153)),
        10 - 15 / 2, 10 - 15 / 2);

    return t.checkExpect(w, result);
  }

  boolean testNextCell(Tester t) {
    Cell a = new Cell(0, 0); // start- top left
    Cell b = new Cell(0, 1);
    Cell c = new Cell(0, 2);
    Cell d = new Cell(1, 0);
    Cell e = new Cell(1, 1);
    Cell f = new Cell(1, 2);
    Cell g = new Cell(2, 0);
    Cell h = new Cell(2, 1);
    Cell i = new Cell(2, 2); // end- bottom right

    ArrayList<Cell> vertices = new ArrayList<>();

    vertices.add(a);
    vertices.add(b);
    vertices.add(c);
    vertices.add(d);
    vertices.add(e);
    vertices.add(f);
    vertices.add(g);
    vertices.add(h);
    vertices.add(i);

    Wall ab = new Wall(a, b, 5);
    Wall ad = new Wall(a, d, 3);
    Wall bc = new Wall(b, c, 7);
    Wall be = new Wall(b, e, 8);
    Wall ba = new Wall(b, a, 2);
    Wall cb = new Wall(c, b, 2);
    Wall cf = new Wall(c, f, 3);
    Wall da = new Wall(d, a, 6);
    Wall de = new Wall(d, e, 4);
    Wall dg = new Wall(d, g, 5);
    Wall eb = new Wall(e, b, 1);
    Wall ef = new Wall(e, f, 3);
    Wall eh = new Wall(e, h, 2);
    Wall ed = new Wall(e, d, 5);
    Wall fc = new Wall(f, c, 6);
    Wall fe = new Wall(f, e, 5);
    Wall fi = new Wall(f, i, 9);
    Wall gd = new Wall(g, d, 7);
    Wall gh = new Wall(g, h, 7);
    Wall hg = new Wall(h, g, 4);
    Wall he = new Wall(h, e, 3);
    Wall hi = new Wall(h, i, 1);
    Wall ih = new Wall(i, h, 3);
    Wall iF = new Wall(i, f, 2);

    ArrayList<Wall> edges = new ArrayList<>();

    edges.add(ab);
    edges.add(ad);
    edges.add(bc);
    edges.add(be);
    edges.add(ba);
    edges.add(cb);
    edges.add(cf);
    edges.add(da);
    edges.add(de);
    edges.add(dg);
    edges.add(eb);
    edges.add(ef);
    edges.add(eh);
    edges.add(ed);
    edges.add(fc);
    edges.add(fe);
    edges.add(fi);
    edges.add(gd);
    edges.add(gh);
    edges.add(hg);
    edges.add(he);
    edges.add(hi);
    edges.add(ih);
    edges.add(iF);

    // found by getting the lowest weighted edges using kruskal's algorithm
    ArrayList<Wall> resultEdgesInTree = new ArrayList<>();
    resultEdgesInTree.add(eb);
    resultEdgesInTree.add(hi);
    resultEdgesInTree.add(ba);
    resultEdgesInTree.add(cb);
    resultEdgesInTree.add(eh);
    resultEdgesInTree.add(iF);
    resultEdgesInTree.add(ad);
    resultEdgesInTree.add(hg);

    // moving from a to d (one move right)
    Cell aToDMove = a.nextCell(resultEdgesInTree, 1, 0, 3, 3);

    // moving from h to g (one move up)
    Cell hToGMove = h.nextCell(resultEdgesInTree, 0, -1, 3, 3);

    // moving from f to c (one move left)
    Cell fToCMove = f.nextCell(resultEdgesInTree, -1, 0, 3, 3);

    // moving from a to out of bounds (one move left)
    Cell aToOutOfBoundsMove = a.nextCell(resultEdgesInTree, -1, 0, 3, 3);

    return t.checkExpect(aToDMove, d) // path in mst, valid
        && t.checkExpect(hToGMove, g) // path in mst, valid
        && t.checkExpect(fToCMove, f) // path not in mst, not valid
        && t.checkExpect(aToOutOfBoundsMove, a); // out of bounds, not valid
  }

  boolean testIsBeginningAndIsEnding(Tester t) {
    int width = 5;
    int height = 5;
    Cell start = new Cell(0, 0);
    Cell b = new Cell(1, 0);
    Cell x = new Cell(5, 4);
    Cell end = new Cell(5, 5);

    return t.checkExpect(start.isBeginning(), true) && t.checkExpect(b.isBeginning(), false)
        && t.checkExpect(x.isEnding(width, height), false)
        && t.checkExpect(end.isEnding(width, height), false);
  }

  boolean testCellCompare(Tester t) {
    Cell center = new Cell(1, 1);
    Cell alsoUp = new Cell(1, 0);
    Cell up = new Cell(1, 0);
    Cell left = new Cell(0, 1);
    Cell right = new Cell(2, 1);
    Cell down = new Cell(1, 2);

    Cell rand = new Cell(5, 5);
    return t.checkExpect(center.cellCompare(up), 1) && t.checkExpect(center.cellCompare(down), -1)
        && t.checkExpect(center.cellCompare(left), 1) && t.checkExpect(up.cellCompare(alsoUp), 0)
        && t.checkExpect(center.cellCompare(right), -1)
        && t.checkExpect(down.cellCompare(rand), -1);
  }

  // ------------------------ MAZE METHODS ---------------------------- //

  boolean testKruskal3x3(Tester t) {

    Cell a = new Cell(0, 0); // start- top left
    Cell b = new Cell(0, 1);
    Cell c = new Cell(0, 2);
    Cell d = new Cell(1, 0);
    Cell e = new Cell(1, 1);
    Cell f = new Cell(1, 2);
    Cell g = new Cell(2, 0);
    Cell h = new Cell(2, 1);
    Cell i = new Cell(2, 2); // end- bottom right

    Wall ab = new Wall(a, b, 5);
    Wall ad = new Wall(a, d, 3);
    Wall bc = new Wall(b, c, 7);
    Wall be = new Wall(b, e, 8);
    Wall ba = new Wall(b, a, 2);
    Wall cb = new Wall(c, b, 2);
    Wall cf = new Wall(c, f, 3);
    Wall da = new Wall(d, a, 6);
    Wall de = new Wall(d, e, 4);
    Wall dg = new Wall(d, g, 5);
    Wall eb = new Wall(e, b, 1);
    Wall ef = new Wall(e, f, 3);
    Wall eh = new Wall(e, h, 2);
    Wall ed = new Wall(e, d, 5);
    Wall fc = new Wall(f, c, 6);
    Wall fe = new Wall(f, e, 5);
    Wall fi = new Wall(f, i, 9);
    Wall gd = new Wall(g, d, 7);
    Wall gh = new Wall(g, h, 7);
    Wall hg = new Wall(h, g, 4);
    Wall he = new Wall(h, e, 3);
    Wall hi = new Wall(h, i, 1);
    Wall ih = new Wall(i, h, 3);
    Wall iF = new Wall(i, f, 2);

    ArrayList<Cell> vertices = new ArrayList<>();

    vertices.add(a);
    vertices.add(b);
    vertices.add(c);
    vertices.add(d);
    vertices.add(e);
    vertices.add(f);
    vertices.add(g);
    vertices.add(h);
    vertices.add(i);

    ArrayList<Wall> edges = new ArrayList<>();

    edges.add(ab);
    edges.add(ad);
    edges.add(bc);
    edges.add(be);
    edges.add(ba);
    edges.add(cb);
    edges.add(cf);
    edges.add(da);
    edges.add(de);
    edges.add(dg);
    edges.add(eb);
    edges.add(ef);
    edges.add(eh);
    edges.add(ed);
    edges.add(fc);
    edges.add(fe);
    edges.add(fi);
    edges.add(gd);
    edges.add(gh);
    edges.add(hg);
    edges.add(he);
    edges.add(hi);
    edges.add(ih);
    edges.add(iF);

    HashMap<Cell, Cell> representatives = new HashMap<>();
    for (Cell z : vertices) {
      representatives.put(z, z);
    }

    ArrayList<Wall> worklist = new ArrayList<>();
    for (int u = 0; u < edges.size(); u += 1) {
      worklist.add(edges.get(u));
    }
    // sort the work list by lowest to highest weights
    Collections.sort(worklist, new Comparator<Wall>() {
      public int compare(Wall w1, Wall w2) {
        if (w1.weightDifference(w2) > 0) {
          return 1;
        }
        else if (w1.weightDifference(w2) < 0) {
          return -1;
        }
        else {
          return 0;
        }
      }
    });

    ArrayList<Wall> mst = new ArrayList<>();

    // while there is more than one tree
    while (mst.size() < vertices.size() - 1) {

      Wall curr = worklist.get(0);

      // get the lowest edge and check if its already connected
      if (curr.alreadyConnected(representatives)) {
        worklist.remove(0); // edge is connected, discard it
      }
      else {
        // non cyclic path, so add it to the mst
        curr.union(representatives, curr);
        mst.add(worklist.remove(0));
      }
    }

    // found by getting the lowest weighted edges using kruskal's algorithm
    ArrayList<Wall> resultEdgesInTree = new ArrayList<>();
    resultEdgesInTree.add(eb);
    resultEdgesInTree.add(hi);
    resultEdgesInTree.add(ba);
    resultEdgesInTree.add(cb);
    resultEdgesInTree.add(eh);
    resultEdgesInTree.add(iF);
    resultEdgesInTree.add(ad);
    resultEdgesInTree.add(hg);

    return t.checkExpect(mst, resultEdgesInTree);
  }

  boolean testGetAllCells(Tester t) {

    ArrayList<Cell> vertices = new ArrayList<>();

    vertices.add(a);
    vertices.add(b);
    vertices.add(c);
    vertices.add(d);
    vertices.add(e);
    vertices.add(f);
    vertices.add(g);
    vertices.add(h);
    vertices.add(i);

    Wall ab = new Wall(a, b, 5);
    Wall ad = new Wall(a, d, 3);
    Wall bc = new Wall(b, c, 7);
    Wall be = new Wall(b, e, 8);
    Wall ba = new Wall(b, a, 2);
    Wall cb = new Wall(c, b, 2);
    Wall cf = new Wall(c, f, 3);
    Wall da = new Wall(d, a, 6);
    Wall de = new Wall(d, e, 4);
    Wall dg = new Wall(d, g, 5);
    Wall eb = new Wall(e, b, 1);
    Wall ef = new Wall(e, f, 3);
    Wall eh = new Wall(e, h, 2);
    Wall ed = new Wall(e, d, 5);
    Wall fc = new Wall(f, c, 6);
    Wall fe = new Wall(f, e, 5);
    Wall fi = new Wall(f, i, 9);
    Wall gd = new Wall(g, d, 7);
    Wall gh = new Wall(g, h, 7);
    Wall hg = new Wall(h, g, 4);
    Wall he = new Wall(h, e, 3);
    Wall hi = new Wall(h, i, 1);
    Wall ih = new Wall(i, h, 3);
    Wall iF = new Wall(i, f, 2);

    ArrayList<Wall> edges = new ArrayList<>();

    edges.add(ab);
    edges.add(ad);
    edges.add(bc);
    edges.add(be);
    edges.add(ba);
    edges.add(cb);
    edges.add(cf);
    edges.add(da);
    edges.add(de);
    edges.add(dg);
    edges.add(eb);
    edges.add(ef);
    edges.add(eh);
    edges.add(ed);
    edges.add(fc);
    edges.add(fe);
    edges.add(fi);
    edges.add(gd);
    edges.add(gh);
    edges.add(hg);
    edges.add(he);
    edges.add(hi);
    edges.add(ih);
    edges.add(iF);

    Cell smallA = new Cell(0, 0);
    Cell smallB = new Cell(1, 0);
    Cell smallC = new Cell(0, 1);
    Cell smallD = new Cell(1, 1);

    ArrayList<Cell> vertices2 = new ArrayList<>();
    vertices2.add(smallA);
    vertices2.add(smallB);
    vertices2.add(smallC);
    vertices2.add(smallD);

    Wall smallAB = new Wall(smallA, smallB, 2);
    Wall smallAC = new Wall(smallA, smallC, 3);
    Wall smallBA = new Wall(smallB, smallA, 1);
    Wall smallBD = new Wall(smallB, smallD, 5);
    Wall smallCA = new Wall(smallC, smallA, 1);
    Wall smallCD = new Wall(smallC, smallD, 3);
    Wall smallDB = new Wall(smallD, smallB, 2);
    Wall smallDC = new Wall(smallD, smallC, 1);

    ArrayList<Wall> edges2 = new ArrayList<>();

    edges2.add(smallAB);
    edges2.add(smallAC);
    edges2.add(smallBA);
    edges2.add(smallBD);
    edges2.add(smallCA);
    edges2.add(smallCD);
    edges2.add(smallDB);
    edges2.add(smallDC);

    Maze m = new Maze(3, 3);

    return t.checkExpect(m.getAllCells(edges), vertices)
        && t.checkExpect(m.getAllCells(edges2), vertices2);
  }


  //------ METHODS IN WALL ------- \\

  boolean testHashCode(Tester t) {
    return t.checkExpect(ab.hashCode(), 1000)
        && t.checkExpect(fe.hashCode(), 20011001)
        && t.checkExpect(hg.hashCode(), 10020002);
  }

  boolean testEqualCells(Tester t) {
    return t.checkExpect(ab.equals(ba), true)
        && t.checkExpect(hg.equals(hi), false)
        && t.checkExpect(ih.equals(hi), true)
        && t.checkExpect(da.equals(fi), false);
  }

  boolean testWeightDifference(Tester t) {
    return t.checkExpect(bc.weightDifference(he), 1)
        && t.checkExpect(bc.weightDifference(cb), 2)
        && t.checkExpect(hi.weightDifference(gd), -2);
  }

  boolean testAlreadyConnected(Tester t) {

    HashMap<Cell, Cell> reps = new HashMap<Cell, Cell>();
    reps.put(a, a);
    reps.put(b, a);
    reps.put(c, a);
    reps.put(d, e);
    reps.put(e, f);
    reps.put(f, f);

    return t.checkExpect(bc.alreadyConnected(reps), true)
        && t.checkExpect(ab.alreadyConnected(reps), true)
        && t.checkExpect(eb.alreadyConnected(reps), false);
  }

  boolean testFind(Tester t) {

    HashMap<Cell, Cell> reps = new HashMap<Cell, Cell>();
    reps.put(a, a);
    reps.put(b, a);
    reps.put(c, a);
    reps.put(d, e);
    reps.put(e, f);
    reps.put(f, f);

    return t.checkExpect(ab.find(reps, a), a)
        && t.checkExpect(bc.find(reps, a), a)
        && t.checkExpect(ef.find(reps, f), f);
  }

  boolean testUnion(Tester t) {

    HashMap<Cell, Cell> reps = new HashMap<Cell, Cell>();
    reps.put(a, a);
    reps.put(b, b);
    reps.put(c, a);
    reps.put(d, e);
    reps.put(e, f);
    reps.put(f, f);

    ab.union(reps, ab);

    HashMap<Cell, Cell> repsResult = new HashMap<Cell, Cell>();
    repsResult.put(a, a);
    repsResult.put(b, a);
    repsResult.put(c, a);
    repsResult.put(d, e);
    repsResult.put(e, f);
    repsResult.put(f, f);

    HashMap<Cell, Cell> reps2 = new HashMap<Cell, Cell>();
    reps2.put(a, a);
    reps2.put(b, a);
    reps2.put(c, a);
    reps2.put(d, e);
    reps2.put(e, e);
    reps2.put(f, f);

    ef.union(reps2, ef);

    HashMap<Cell, Cell> repsResult2 = new HashMap<Cell, Cell>();
    repsResult2.put(a, a);
    repsResult2.put(b, a);
    repsResult2.put(c, a);
    repsResult2.put(d, e);
    repsResult2.put(e, e);
    repsResult2.put(f, e);

    return t.checkExpect(reps, repsResult)
        && t.checkExpect(reps2, repsResult2);
  }

  boolean testAddWallCells(Tester t) {

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);

    gh.addWallCells(cells);

    ArrayList<Cell> cellsResult = new ArrayList<Cell>();
    cellsResult.add(a);
    cellsResult.add(b);
    cellsResult.add(c);
    cellsResult.add(d);
    cellsResult.add(e);
    cellsResult.add(f);
    cellsResult.add(h);
    cellsResult.add(g);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells2.add(a);
    cells2.add(b);
    cells2.add(c);
    cells2.add(d);
    cells2.add(e);
    cells2.add(f);

    dg.addWallCells(cells2);

    ArrayList<Cell> cellsResult2 = new ArrayList<Cell>();
    cellsResult2.add(a);
    cellsResult2.add(b);
    cellsResult2.add(c);
    cellsResult2.add(d);
    cellsResult2.add(e);
    cellsResult2.add(f);
    cellsResult2.add(g);

    ArrayList<Cell> cells3 = new ArrayList<Cell>();
    cells3.add(a);
    cells3.add(b);
    cells3.add(c);
    cells3.add(d);
    cells3.add(e);
    cells3.add(f);

    bc.addWallCells(cells3);

    return t.checkExpect(cells, cellsResult)
        && t.checkExpect(cells2, cellsResult2)
        && t.checkExpect(cells3, cells3);
  }

  boolean testAddUniqueCells(Tester t) {

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);

    ab.addUniqueCells(cells, a);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells2.add(a);
    cells2.add(b);
    cells2.add(c);
    cells2.add(d);
    cells2.add(e);
    cells2.add(f);

    gh.addUniqueCells(cells2, h);

    ArrayList<Cell> cells2Result = new ArrayList<Cell>();
    cells2Result.add(a);
    cells2Result.add(b);
    cells2Result.add(c);
    cells2Result.add(d);
    cells2Result.add(e);
    cells2Result.add(f);
    cells2Result.add(h);

    return t.checkExpect(cells, cells)
        && t.checkExpect(cells2, cells2Result);
  }

  boolean testDrawWall(Tester t) {

    WorldScene w = new WorldScene(15, 15);
    WorldScene result = new WorldScene(15, 15);

    ab.drawWall(w);
    result.placeImageXY(new RectangleImage(15, 1, OutlineMode.SOLID, Color.black), 15 / 2, 15);
    de.drawWall(w);
    result.placeImageXY(new RectangleImage(1, 15, OutlineMode.SOLID, Color.black), 15, 15 + 15 / 2);
    da.drawWall(w);
    result.placeImageXY(new RectangleImage(15, 1, OutlineMode.SOLID, Color.black), 15 + 15 / 2, 15);

    return t.checkExpect(w, result);
  }

  public boolean testDirection(Tester t) {

    return t.checkExpect(ab.direction(), "east") 
        && t.checkExpect(bc.direction(), "east")
        && t.checkExpect(gd.direction(), "north") 
        && t.checkExpect(ih.direction(), "west")
        && t.checkExpect(cf.direction(), "south");
  }

  // ----------- METHODS IN MAZE ----------- \\ 

  boolean testCreateMap(Tester t) {

    Maze maze = new Maze(3, 3);

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);

    HashMap<Cell, Cell> reps = new HashMap<Cell, Cell>();
    reps.put(a, a);
    reps.put(b, b);
    reps.put(c, c);
    reps.put(d, d);
    reps.put(e, e);
    reps.put(f, f);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells.add(a);
    cells.add(a);
    cells.add(a);
    cells.add(b);
    cells.add(c);

    HashMap<Cell, Cell> reps2 = new HashMap<Cell, Cell>();
    reps.put(a, a);
    reps.put(b, b);
    reps.put(c, c);

    return t.checkExpect(maze.createMap(cells), reps)
        && t.checkExpect(maze.createMap(cells2), reps2);
  }

  boolean testFindStart(Tester t) {
    Maze maze = new Maze(3, 3);

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);

    return t.checkExpect(maze.findStart(cells), a);
  }

  boolean testFindEnd(Tester t) {
    Maze maze = new Maze(3, 3);

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);

    return t.checkExpect(maze.findEnd(cells), i);
  }

  boolean sameWalls(ArrayList<Wall> mazeWalls, ArrayList<Wall> walls) {
    for(int i = 0; i < mazeWalls.size(); i += 1) {

      if (!mazeWalls.get(i).equals(walls.get(i))) {

        return false;
      }
    }
    return true;
  }

  boolean testCreateEdges3x3(Tester t) {
    Maze maze = new Maze(3, 3, new Random());

    ArrayList<Wall> walls = new ArrayList<Wall>();

    walls.add(ad);
    walls.add(ab);
    walls.add(be);
    walls.add(bc);
    walls.add(cf);
    walls.add(dg);
    walls.add(de);
    walls.add(eh);
    walls.add(ef);
    walls.add(fi);
    walls.add(gh);
    walls.add(hi);  

    ArrayList<Wall> mazeWalls = maze.createEdges();


    //TODO ASK
    return t.checkExpect(this.sameWalls(mazeWalls, walls), true);

  }

  boolean testCreateEdges2x2(Tester t) {
    Maze maze = new Maze(2, 2, new Random());

    ArrayList<Wall> walls = new ArrayList<Wall>();

    walls.add(wy);
    walls.add(wx);
    walls.add(xz);
    walls.add(yz);

    ArrayList<Wall> mazeWalls = maze.createEdges();

    //TODO ASK
    return t.checkExpect(this.sameWalls(mazeWalls, walls), true);
  }

  boolean testAddUniqueEdges(Tester t) {

    Maze maze = new Maze(3, 3, new Random());
    Maze maze2 = new Maze(3, 3, new Random());

    ArrayList<Wall> walls = new ArrayList<Wall>();
    walls.add(ab);
    walls.add(be);
    walls.add(cf);
    walls.add(dg);
    walls.add(eh);
    walls.add(fi);

    maze.addUniqueEdges(walls, ab);

    ArrayList<Wall> walls2 = new ArrayList<Wall>();
    walls2.add(ab);
    walls2.add(be);
    walls2.add(cf);
    walls2.add(dg);
    walls2.add(eh);
    walls2.add(fi);

    maze2.addUniqueEdges(walls2, gh);

    ArrayList<Wall> walls2Result = new ArrayList<Wall>();
    walls2Result.add(ab);
    walls2Result.add(be);
    walls2Result.add(cf);
    walls2Result.add(dg);
    walls2Result.add(eh);
    walls2Result.add(fi);
    walls2Result.add(gh);

    return t.checkExpect(walls, walls)
        && t.checkExpect(walls2, walls2Result);
  }

  boolean testGetAllCellsInMaze(Tester t) {

    Maze maze = new Maze(3, 3, new Random());
    Maze maze2 = new Maze(2, 2, new Random());

    ArrayList<Wall> walls = new ArrayList<Wall>();

    walls.add(ad);
    walls.add(ab);
    walls.add(be);
    walls.add(bc);
    walls.add(cf);
    walls.add(dg);
    walls.add(de);
    walls.add(eh);
    walls.add(ef);
    walls.add(fi);
    walls.add(gh);
    walls.add(hi);  

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);

    ArrayList<Wall> walls2 = new ArrayList<Wall>();

    walls2.add(wx);
    walls2.add(wy);
    walls2.add(xw);
    walls2.add(xz);
    walls2.add(zx);
    walls2.add(zy);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells2.add(w);
    cells2.add(x);
    cells2.add(y);
    cells2.add(z);

    return t.checkExpect(maze.getAllCells(walls), cells)
        && t.checkExpect(maze2.getAllCells(walls2), cells2);
  }

  boolean testNextElem(Tester t) {

    ArrayList<Wall> walls = new ArrayList<Wall>();

    walls.add(ad);
    walls.add(ab);
    walls.add(be);
    walls.add(bc);
    walls.add(cf);
    walls.add(dg);
    walls.add(de);
    walls.add(eh);
    walls.add(ef);
    walls.add(fi);
    walls.add(gh);
    walls.add(hi); 

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);

    ArrayList<Wall> walls2 = new ArrayList<Wall>();
    walls2.add(wx);
    walls2.add(wy);
    walls2.add(xw);
    walls2.add(xz);
    walls2.add(zx);
    walls2.add(zy);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells2.add(w);
    cells2.add(x);
    cells2.add(y);
    cells2.add(z);

    ICollection<Cell> worklist = new Queue<Cell>();
    ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();


    ICollection<Cell> worklist2 = new Stack<Cell>();
    worklist2.add(z);

    ArrayList<Cell> alreadySeen2 = new ArrayList<Cell>();
    alreadySeen2.add(w);
    alreadySeen2.add(x);
    alreadySeen2.add(y);

    HashMap<Cell, Cell> cameFromEdge2 = new HashMap<Cell, Cell>();
    cameFromEdge2.put(w, w);
    cameFromEdge2.put(x, w);
    cameFromEdge2.put(z, x);

    Maze maze = new Maze(3, 3, walls, cells, worklist, alreadySeen, cameFromEdge);
    Maze maze2 = new Maze(2, 2, walls2, cells2, worklist2, alreadySeen2, cameFromEdge2);


    return t.checkExpect(maze.nextElem(), false)
        && t.checkExpect(maze2.nextElem(), true);
  }

  boolean testSolution(Tester t) {

    ArrayList<Wall> walls = new ArrayList<Wall>();
    walls.add(ad);
    walls.add(ab);
    walls.add(be);
    walls.add(bc);
    walls.add(cf);
    walls.add(dg);
    walls.add(de);
    walls.add(eh);
    walls.add(ef);
    walls.add(fi);
    walls.add(gh);
    walls.add(hi); 

    ArrayList<Cell> cells = new ArrayList<Cell>();
    cells.add(a);
    cells.add(b);
    cells.add(c);
    cells.add(d);
    cells.add(e);
    cells.add(f);
    cells.add(g);
    cells.add(h);
    cells.add(i);

    ArrayList<Wall> walls2 = new ArrayList<Wall>();
    walls2.add(wx);
    walls2.add(wy);
    walls2.add(xw);
    walls2.add(xz);
    walls2.add(zx);
    walls2.add(zy);

    ArrayList<Cell> cells2 = new ArrayList<Cell>();
    cells2.add(w);
    cells2.add(x);
    cells2.add(y);
    cells2.add(z);
    
    Cell aCopy = new Cell(0, 0); // start- top left
    Cell bCopy = new Cell(1, 0);
    Cell cCopy = new Cell(2, 0);
    Cell dCopy = new Cell(0, 1);
    Cell eCopy = new Cell(1, 1);
    Cell fCopy = new Cell(2, 1);
    Cell gCopy = new Cell(0, 2);
    Cell hCopy = new Cell(1, 2);
    Cell iCopy = new Cell(2, 2); // end- bottom right

    Wall abCopy = new Wall(a, b, 8);
    Wall adCopy = new Wall(a, d, 5);
    Wall bcCopy = new Wall(b, c, 4);
    Wall beCopy = new Wall(b, e, 0);
    
    Wall cfCopy = new Wall(c, f, 1);
    Wall deCopy = new Wall(d, e, 1);
    Wall dgCopy = new Wall(d, g, 2);
    Wall efCopy = new Wall(e, f, 0);
    Wall ehCopy = new Wall(e, h, 5);
    Wall fiCopy = new Wall(f, i, 2);
    Wall ghCopy = new Wall(g, h, 7);
    Wall hiCopy = new Wall(h, i, 5);
   
    
    Cell wCopy = new Cell(0, 0);
    Cell xCopy = new Cell(1, 0);
    Cell yCopy = new Cell(0, 1);
    Cell zCopy = new Cell(1, 1);

    Wall wxCopy = new Wall(w, x, 3);
    Wall wyCopy = new Wall(w, y, 2);
    Wall xwCopy = new Wall(x, w, 4);
    Wall xzCopy = new Wall(x, z, 1);
    Wall zxCopy = new Wall(z, x, 5);
    Wall zyCopy = new Wall(z, y, 2);

    ICollection<Cell> worklist = new Queue<Cell>();
    worklist.add(i);
    ArrayList<Cell> alreadySeen = new ArrayList<Cell>();
    alreadySeen.add(a);
    alreadySeen.add(b);
    alreadySeen.add(c);
    alreadySeen.add(d);
    alreadySeen.add(e);
    alreadySeen.add(f);
    alreadySeen.add(g);
    alreadySeen.add(h);
    HashMap<Cell, Cell> cameFromEdge = new HashMap<Cell, Cell>();
    cameFromEdge.put(a, a);
    cameFromEdge.put(d, a);
    cameFromEdge.put(e, d);
    cameFromEdge.put(f, e);
    cameFromEdge.put(i, f);

    ICollection<Cell> worklist2 = new Stack<Cell>();
    worklist2.add(z);

    ArrayList<Cell> alreadySeen2 = new ArrayList<Cell>();
    alreadySeen2.add(w);
    alreadySeen2.add(x);
    alreadySeen2.add(y);

    HashMap<Cell, Cell> cameFromEdge2 = new HashMap<Cell, Cell>();
    cameFromEdge2.put(w, w);
    cameFromEdge2.put(x, w);
    cameFromEdge2.put(z, x);

    Maze maze = new Maze(3, 3, walls, cells, worklist, alreadySeen, cameFromEdge);
    Maze maze2 = new Maze(2, 2, walls2, cells2, worklist2, alreadySeen2, cameFromEdge2);

    WorldScene w1 = new WorldScene(45, 45);
    Color gray = new Color(204, 204, 204);
    Color lightBlue = new Color(51, 204, 255);
    Color darkBlue = new Color(0, 0, 220);
    Color start = new Color(255, 255, 153);
    Color end = new Color(102, 0, 153);
    
    aCopy.drawCell(w1, gray);
    bCopy.drawCell(w1, lightBlue);
    cCopy.drawCell(w1, lightBlue);
    dCopy.drawCell(w1, darkBlue);
    eCopy.drawCell(w1, darkBlue);
    fCopy.drawCell(w1, darkBlue);
    gCopy.drawCell(w1, lightBlue);
    hCopy.drawCell(w1, lightBlue);
    iCopy.drawCell(w1, end);

    adCopy.drawWall(w1);
    abCopy.drawWall(w1);
    beCopy.drawWall(w1);
    bcCopy.drawWall(w1);
    cfCopy.drawWall(w1);
    dgCopy.drawWall(w1);
    deCopy.drawWall(w1);
    ehCopy.drawWall(w1);
    efCopy.drawWall(w1);
    fiCopy.drawWall(w1);
    ghCopy.drawWall(w1);
    hiCopy.drawWall(w1);
    
    WorldScene w2 = new WorldScene(30, 30);
    wCopy.drawCell(w2, start);
    xCopy.drawCell(w2, darkBlue);
    yCopy.drawCell(w2, lightBlue);
    zCopy.drawCell(w2, end);

    wxCopy.drawWall(w2);
    wyCopy.drawWall(w2);
    xwCopy.drawWall(w2);
    xzCopy.drawWall(w2);
    zxCopy.drawWall(w2);
    zyCopy.drawWall(w2);

    maze.solution();
    maze2.solution();
    WorldScene drawnMaze = maze.makeScene();
    WorldScene drawnMaze2 = maze2.makeScene();

    return t.checkExpect(drawnMaze, w1)
        && t.checkExpect(drawnMaze2, w2);
  }

  // -------------- IN DEQUE, STACK, AND QUEUE -------------- \\


  Deque<String> deque1 = new Deque<String>();
  Sentinel<String> mtSentinel =  new Sentinel<String>();

  Deque<String> deque2 = new Deque<String>();
  Sentinel<String> sentinel2 =  new Sentinel<String>();

  Deque<String> deque3 = new Deque<String>();
  Sentinel<String> sentinel3 =  new Sentinel<String>();


  //we have node assignments here for testing purposes
  Deque<String> dequeTest = new Deque<String>();
  Sentinel<String> sentinelTest = new Sentinel<String>();

  ANode<String> abcTest = new Node<String>("abc");
  ANode<String> bcdTest = new Node<String>("bcd");
  ANode<String> cdeTest = new Node<String>("cde");
  ANode<String> defTest = new Node<String>("def");

  //tail test
  Deque<String> dequeT = new Deque<String>();
  Sentinel<String> sentinelT =  new Sentinel<String>();

  ANode<String> seaT = new Node<String>("sea");
  ANode<String> abcT = new Node<String>("abc");
  ANode<String> bcdT = new Node<String>("bcd");
  ANode<String> cdeT = new Node<String>("cde");
  ANode<String> defT = new Node<String>("def");


  //head test
  Deque<String> dequeH = new Deque<String>();
  Sentinel<String> sentinelH =  new Sentinel<String>();

  ANode<String> abcH = new Node<String>("abc");
  ANode<String> bcdH = new Node<String>("bcd");
  ANode<String> cdeH = new Node<String>("cde");
  ANode<String> defH = new Node<String>("def");
  ANode<String> teaH = new Node<String>("tea");

  //INITIAL CONDITIONS
  void init() {
    this.mtSentinel = new Sentinel<String>();
    this.sentinel2 = new Sentinel<String>();
    this.sentinel2 = new Sentinel<String>();
    this.sentinel3 = new Sentinel<String>();
    this.sentinelTest = new Sentinel<String>();
    this.sentinelT = new Sentinel<String>();
    this.sentinelH = new Sentinel<String>();

    this.deque2.addAtTail("abc");
    this.deque2.addAtTail("bcd");
    this.deque2.addAtTail("cde");
    this.deque2.addAtTail("def");

    this.deque3.addAtTail("cat");
    this.deque3.addAtTail("bat");
    this.deque3.addAtTail("pal");
    this.deque3.addAtTail("sat");
    this.deque3.addAtTail("sea");
    this.deque3.addAtTail("tea"); 

    this.dequeTest.addAtTail("abc");
    this.dequeTest.addAtTail("bcd");
    this.dequeTest.addAtTail("cde");
    this.dequeTest.addAtTail("def");

    this.deque1 = new Deque<String>(mtSentinel);

    this.deque2 = new Deque<String>(sentinel2);

    this.deque3 = new Deque<String>(sentinel3);

    this.dequeTest = new Deque<String>(sentinelTest);

    this.dequeT = new Deque<String>(sentinelT);

    this.dequeH = new Deque<String>(sentinelH);
  }

  //we have node assignments here for testing purposes
  void assignmentConds() {
    this.abcTest = new Node<String>("abc", this.bcdTest, sentinelTest);
    this.bcdTest = new Node<String>("bcd", this.cdeTest, this.abcTest);
    this.cdeTest = new Node<String>("cde", this.defTest, this.bcdTest);
    this.defTest = new Node<String>("def", sentinelTest, this.cdeTest); 
    this.dequeTest = new Deque<String>(sentinelTest);

    this.abcT = new Node<String>("abc", this.bcdT, sentinelT);
    this.bcdT = new Node<String>("bcd", this.cdeT, this.abcT);
    this.cdeT = new Node<String>("cde", this.defT, this.bcdT);
    this.defT = new Node<String>("def", this.seaT, this.cdeT);
    this.seaT = new Node<String>("sea", sentinelT, this.defT);
    this.dequeT = new Deque<String>(sentinelT);

    this.teaH = new Node<String>("tea", this.abcH, sentinelH);
    this.abcH = new Node<String>("abc", this.bcdH, this.teaH);
    this.bcdH = new Node<String>("bcd", this.cdeH, this.abcH);
    this.cdeH = new Node<String>("cde", this.defH, this.bcdH);
    this.defH = new Node<String>("def", sentinelH, this.cdeH);
    this.dequeH = new Deque<String>(sentinelH);
  } 



  //FUNCTION OBJECTS FOR TESTING
  //3 letter deques
  class IsThreeLetters implements Predicate<String> {
    public boolean test(String s) {
      return (s.length() == 3);
    }
  }

  //4 letter deques (to have a false predicate)
  class IsFourLetters implements Predicate<String> {
    public boolean test(String s) {
      return (s.length() == 4);
    }
  }

  //Deque 2 middle
  class IsBcd implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("bcd"));
    }
  }

  //Deque 3 middle
  class IsSea implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("sea"));
    }
  }

  //Deque 2 head
  class IsAbc implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("abc"));
    }
  }

  //Deque 3 head
  class IsCat implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("cat"));
    }
  }

  //Deque 2 tail
  class IsDef implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("def"));
    }
  }

  //Deque 3 tail
  class IsTea implements Predicate<String> {
    public boolean test(String s) {
      return (s.equals("tea"));
    }
  }


  //TESTING METHODS BELOW:
  //SIZE
  boolean testSize(Tester t) {
    this.init();


    //check an empty deque
    boolean beforeAdding = t.checkExpect(this.deque1.size(), 0)

        //check non-empty deques
        && t.checkExpect(this.deque2.size(), 0)
        && t.checkExpect(this.deque3.size(), 0)
        && t.checkExpect(this.dequeTest.size(), 0);


    this.assignmentConds();

    //check an empty deque
    boolean afterAdding = t.checkExpect(this.deque1.size(), 0)
        //check non-empty deques
        //&& t.checkExpect(this.deque2.size(), 4)
        //&& t.checkExpect(this.deque3.size(), 6)
        && t.checkExpect(this.dequeTest.size(), 4);

    return beforeAdding
        && afterAdding;
  }

  //REMOVE FROM HEAD: test runtime exception
  boolean testRemoveFromHeadEmpty(Tester t) {
    this.init();
    this.assignmentConds();
    //check runtime exception
    return t.checkException(
        new RuntimeException("Cannot remove an element from an empty list"), 
        this.deque1, "removeFromHead");
  }

  //REMOVE FROM HEAD:
  boolean testRemoveFromHead(Tester t) {
    this.init();
    // this.makeDeques();
    this.assignmentConds();

    //check size before removing from head
    boolean sizeDequeTest = t.checkExpect(this.dequeTest.size(), 4);

    //remove the head
    this.dequeTest.removeFromHead();

    //check if size - 1
    boolean sizeDequeTestWithoutHead = t.checkExpect(this.dequeTest.size(), 3);


    //return tests
    return sizeDequeTest && sizeDequeTestWithoutHead;
  }


  //ADD AT HEAD
  boolean testAddAtHead(Tester t) {
    this.init();
    this.assignmentConds();

    //check size
    boolean size = t.checkExpect(this.dequeTest.size(), 4);

    //add tea to head
    this.dequeTest.addAtHead("tea");

    //check that size + 1
    boolean sizeTea = t.checkExpect(this.dequeTest.size(), 5);

    //check that it matches the head version
    boolean head = t.checkExpect(this.dequeTest, this.dequeH);

    //return tests
    return size
        && sizeTea
        && head;
  }

  //ADD AT TAIL
  boolean testAddAtTail(Tester t) {
    this.init();
    this.assignmentConds();

    //check size
    boolean sizeAbc = t.checkExpect(this.dequeTest.size(), 4);

    //add sea to tail
    this.dequeTest.addAtTail("sea");

    //check that size + 1
    boolean sizeSea = t.checkExpect(this.dequeTest.size(), 5);

    //check that it matches the tail version
    boolean tail = t.checkExpect(this.dequeTest, this.dequeT);

    //return tests
    return sizeAbc
        && sizeSea
        && tail;
  }

  // TODO add tests for contains

  boolean testIsEmptyStack(Tester t) {

    Stack<Cell> nonEmpty = new Stack<Cell>();
    nonEmpty.add(a);
    nonEmpty.add(b);
    nonEmpty.add(c);

    Stack<Cell> empty = new Stack<Cell>();

    return t.checkExpect(nonEmpty.isEmpty(), false)
        && t.checkExpect(empty.isEmpty(), true);

  }

  boolean testIsEmptyQueue(Tester t) {

    Queue<Cell> nonEmpty = new Queue<Cell>();
    nonEmpty.add(a);
    nonEmpty.add(b);
    nonEmpty.add(c);

    Queue<Cell> empty = new Queue<Cell>();

    return t.checkExpect(nonEmpty.isEmpty(), false)
        && t.checkExpect(empty.isEmpty(), true);

  }

  boolean testAddStack(Tester t) {

    Stack<Cell> abc = new Stack<Cell>();
    abc.add(a);
    abc.add(b);
    abc.add(c);

    //check size
    boolean size = t.checkExpect(abc.contents.size(), 3);

    //add g to head
    abc.add(g);

    //check that size + 1
    boolean sizeTea = t.checkExpect(abc.contents.size(), 4);

    Stack<Cell> gabc = new Stack<Cell>();
    gabc.add(a);
    gabc.add(b);
    gabc.add(c);
    gabc.contents.addAtHead(g);

    //check that it matches the head version
    boolean head = t.checkExpect(abc, gabc);

    //return tests
    return size
        && sizeTea
        && head;
  }

  boolean testAddQueue(Tester t) {

    Queue<Cell> abc = new Queue<Cell>();
    abc.add(a);
    abc.add(b);
    abc.add(c);

    //check size
    boolean size = t.checkExpect(abc.contents.size(), 3);

    //add g to tail
    abc.add(g);

    //check that size + 1
    boolean sizeTea = t.checkExpect(abc.contents.size(), 4);

    Queue<Cell> gabc = new Queue<Cell>();
    gabc.add(a);
    gabc.add(b);
    gabc.add(c);
    gabc.contents.addAtTail(g);

    //check that it matches the tail version
    boolean head = t.checkExpect(abc, gabc);

    //return tests
    return size
        && sizeTea
        && head;
  }

  boolean testRemoveStack(Tester t) {

    Stack<Cell> abc = new Stack<Cell>();
    abc.add(a);
    abc.add(b);
    abc.add(c);

    //check size
    boolean size = t.checkExpect(abc.contents.size(), 3);

    // remove
    abc.remove();

    //check that size - 1
    boolean sizeTea = t.checkExpect(abc.contents.size(), 2);

    Stack<Cell> bc = new Stack<Cell>();
    bc.add(a);
    bc.add(b);
    bc.add(c);
    bc.contents.removeFromHead();

    //check that it matches the expected version
    boolean removed = t.checkExpect(abc, bc);

    //return tests
    return size
        && sizeTea
        && removed;
  }

  boolean testRemoveQueue(Tester t) {

    Queue<Cell> abc = new Queue<Cell>();
    abc.add(a);
    abc.add(b);
    abc.add(c);

    //check size
    boolean size = t.checkExpect(abc.contents.size(), 3);

    // remove
    abc.remove();

    //check that size - 1
    boolean sizeTea = t.checkExpect(abc.contents.size(), 2);

    Queue<Cell> bc = new Queue<Cell>();
    bc.add(a);
    bc.add(b);
    bc.add(c);
    bc.contents.removeFromHead();

    //check that it matches the expected version
    boolean removed = t.checkExpect(abc, bc);

    //return tests
    return size
        && sizeTea
        && removed;
  }

}
