import java.util.Vector;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

public class AndreyVagin {
    
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        
        String generatingMode;
        do {
            cleanConsole();
            System.out.println("\n\nPlease choose the method of map generating:\n" + 
                                "-Enter 1 to generate map randomly\n" +
                                "-Enter 2 to  insert the positions of agents by yourself\n");
            generatingMode = scanner.nextLine();
        } while(!(generatingMode.equals("1") || generatingMode.equals("2")));
        
        /** library contain world map that will be generated or entered by user */
        Library library = null;

        if (generatingMode.equals("1")) {
            library = new RandomLibrary();
        }
        else {
            cleanConsole();
            System.out.println("Now you can enter all necessary coordinates as in the example below:\n" +
                            "[0,0] [4,2] [2,7] [7,4] [0,8] [1,4]\n\n");
            library = readPositions(library);
        }


        String visionMode;
        do {
            cleanConsole();
            System.out.println("\n\nPlease choose the vision mode:\n" + 
                                "-Enter 1 to use first vision mode\n" +
                                "-Enter 2 to use second vision mode\n" + 
                                "-Enter 3 to simulate algorithms with both vision modes\n\n");
            visionMode = scanner.nextLine();
        } while(!(visionMode.equals("1") || visionMode.equals("2") || visionMode.equals("3")));
        
        
        Harry harry = new Harry(library);
        System.out.println("World Map:\n");
        library.printWorldMap();


        if (visionMode.equals("1")) {
            harry.launchBacktracking(1);
            System.out.println("\n\n");
            harry.launchBFS(1);
        }
        else if (visionMode.equals("2")) {
            harry.launchBacktracking(2);
            System.out.println("\n\n");
            harry.launchBFS(2);
        }
        else {
            System.out.println("Vision variant 1:\n");
            harry.launchBacktracking(1);
            System.out.println("\n\n");
            harry.launchBFS(1);

            System.out.println("Vision variant 2:\n");
            harry.launchBacktracking(2);
            System.out.println("\n\n");
            harry.launchBFS(2);
        }


        scanner.close();
    }


    static void cleanConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /** 
     * Used to control user input for wolrd map
     * 
     * @param library - reference to the created map
     * @return library - reference to the created map
     * 
     * Such a construction is needed for recursive exception handling
     */
    static Library readPositions(Library library) {
        Scanner scanner = new Scanner(System.in);
        String positions = scanner.nextLine();
        
        try {
            boolean check = true;
            library = new GivenLibrary();
            int garryX = (int) positions.charAt(1) - 48;
            int garryY = (int) positions.charAt(3) - 48;
            check = check && ((GivenLibrary) library).insertGarry(garryX, garryY);
            int filchX = (int) positions.charAt(7) - 48;
            int filchY = (int) positions.charAt(9) - 48;
            check = check && ((GivenLibrary) library).insertFilch(filchX, filchY);
            int norrisX = (int) positions.charAt(13) - 48;
            int norrisY = (int) positions.charAt(15) - 48;
            check = check && ((GivenLibrary) library).insertNorris(norrisX, norrisY);
            int bookX = (int) positions.charAt(19) - 48;
            int bookY = (int) positions.charAt(21) - 48;
            check = check && ((GivenLibrary) library).insertBook(bookX, bookY);
            int cloakX = (int) positions.charAt(25) - 48;
            int cloakY = (int) positions.charAt(27) - 48;
            check = check && ((GivenLibrary) library).insertCloak(cloakX, cloakY);
            int exitX = (int) positions.charAt(31) - 48;
            int exitY = (int) positions.charAt(33) - 48;
            check = check && ((GivenLibrary) library).insertExit(exitX, exitY);

            if (check == false) {
                throw new Exception();
            }

        } catch(Exception e) {
            cleanConsole();
            System.out.println("Your input is invalid, please try again\n" +
                                "Here is the example:\n" +
                                "[0,0] [4,2] [2,7] [7,4] [0,8] [1,4]\n\n");
            library = readPositions(library);
        }
        return library;
    }

    /** Const value defining the library size */
    static final int LIB_SIZE = 9;
    
    /** Represents cell content for library map */
    enum CellContent {
        _____,
        HARRY,
        BOOK_,
        EXIT_,
        VISIT, // Cell which Harry has alredy visited (used for backtracking)
        NORRI,
        FILCH,
        ZONE_, // Forbidden zones spanned by Filch and Mrs Norris
        CLOAK,
        QUEUE  // Mark cell as placed into the queue (used for bfs algorithm)
    }

    /** 
     * Represets world map 
     * Used to initialize Harry class
     */
    static abstract class Library {
        /** 
         * Key variable of the class
         * Used for executing both algorithms
         * Keeps Filch, Norris, and their zones
         */
        CellContent[][] map;

        /** Keeps Harry's coordinates */
        public Tuple harry = new Tuple(LIB_SIZE - 1, 0);

        /** Keeps Filch's's coordinates */
        Tuple filch;

        /** Keeps Norris' coordinates */
        Tuple norris;

        /** Keeps book's coordinates */
        Tuple book;

        /** Keeps exit's coordinates */
        Tuple exit;

        /** Keeps cloak's coordinates */
        Tuple cloak;


        /**
         * Prints the given map
         * @param map - map to print
         */
        public void printMap(CellContent[][] map) {
            for (int i = 0; i < LIB_SIZE; ++i) {
                for (int j = 0; j < LIB_SIZE; ++j) {
                    System.out.print(map[i][j] + " ");
                }
                System.out.println();
                System.out.println();
            }
            System.out.println();
            System.out.println();
        }

        /** Creates and prints to console the library objects */
        public void printWorldMap() {
            CellContent[][] tmpMap = this.copyMap(map);
            tmpMap[harry.x][harry.y] = CellContent.HARRY;
            tmpMap[book.x][book.y] = CellContent.BOOK_;
            tmpMap[exit.x][exit.y] = CellContent.EXIT_;
            tmpMap[cloak.x][cloak.y] = CellContent.CLOAK;
            this.printMap(tmpMap);
        }

        /** Creates empty map initialized by "_____" enum value */
        public CellContent[][] createEmptyMap() {
            CellContent[][] map = new CellContent[LIB_SIZE][LIB_SIZE];
            for (int i = 0; i < LIB_SIZE; ++i) {
                for (int j = 0; j < LIB_SIZE; ++j) {
                    map[i][j] = CellContent._____;
                }
            }
            return map;
        }

        /**
         * Creates deep copy of the given map
         * @param map - map to copy
         * @return created copy
         */
        public CellContent[][] copyMap(CellContent[][] map) {
            CellContent[][] copy = new CellContent[LIB_SIZE][LIB_SIZE];
            for (int i = 0; i < LIB_SIZE; ++i) {
                for (int j = 0; j < LIB_SIZE; ++j) {
                    copy[i][j] = map[i][j];
                }
            }
            return copy;
        }

        /**
         * Cleans given map from "VISIT" marks
         * Needed for backtracing algorithm
         * @param map - map to proccess
         * @return cleaned result
         */
        public CellContent[][] cleanFromVisitedMarkers(CellContent[][] map) {
            for (int i = 0; i < LIB_SIZE; ++i) {
                for (int j = 0; j < LIB_SIZE; ++j) {
                    if (map[i][j] == CellContent.VISIT) {
                        map[i][j] = CellContent._____;
                    }
                }
            }
            return map;
        }


        public CellContent[][] getMap() {
            return map;
        }
    }

    /** Class which is responsible for creating randomly generated library map */
    static class RandomLibrary extends Library {
        Random rand = new Random();

        /** Consistent call for functions which place needed elements */
        public RandomLibrary() {
            map = createEmptyMap();
            insertFilch();
            insertNorris();
            insertBook();
            insertExit();
            insertCloak();
        }

        
        void insertFilch() {
            CellContent[][] tmpMap;
            do {
                tmpMap = this.copyMap(map);
                filch = insertZone(2, tmpMap);
            } while(tmpMap[harry.x][harry.y] != CellContent._____);
            map = tmpMap;
        }
        
        
        void insertNorris() {
            CellContent[][] tmpMap;
            do {
                tmpMap = this.copyMap(map);
                norris = insertZone(1, tmpMap);
                tmpMap[norris.x][norris.y] = CellContent.NORRI;
                tmpMap[filch.x][filch.y] = CellContent.FILCH;

            } while(tmpMap[harry.x][harry.y] != CellContent._____ && norris.equals(filch) == false);
            map = tmpMap;
        }
        

        Tuple insertZone(int zoneSize, CellContent[][] map) {
            int index = rand.nextInt(LIB_SIZE * LIB_SIZE);
            int x = index / LIB_SIZE;
            int y = index % LIB_SIZE;
            
            for (int i = 0; i < 1 + zoneSize * 2; ++i) {
                for (int j = 0; j < 1 + zoneSize * 2; ++j) {
                    int u = x + i - zoneSize;
                    int v = y + j - zoneSize;
                    if (isInsideTheLibrary(u, v)) {
                        map[u][v] = CellContent.ZONE_;
                    }
                }
            }

            return new Tuple(x, y);
        }

        
        void insertBook() {
            int index, x, y;
            do {
                index = rand.nextInt(LIB_SIZE * LIB_SIZE);
                x = index / LIB_SIZE;
                y = index % LIB_SIZE;
                book = new Tuple(x, y);
            } while(map[x][y] != CellContent._____);
        }
    
        
        void insertExit() {
            int index, x, y;
            do {
                index = rand.nextInt(LIB_SIZE * LIB_SIZE);
                x = index / LIB_SIZE;
                y = index % LIB_SIZE;
                exit = new Tuple(x, y);
            } while(map[x][y] == CellContent.NORRI ||
                    map[x][y] == CellContent.FILCH ||
                    exit.equals(book) == true);
        }
        
        
        void insertCloak() {
            int index, x, y;
            do {
                index = rand.nextInt(LIB_SIZE * LIB_SIZE);
                x = index / LIB_SIZE;
                y = index % LIB_SIZE;
                cloak = new Tuple(x, y);
            } while(map[x][y] != CellContent._____);
        }
    }

    /** Class to handle manual input of library objects */
    static class GivenLibrary extends Library {

        public GivenLibrary() {
            map = createEmptyMap();
        }

        /**
         * Method to place Harry
         * Checks all necessary conditions
         * @param x - x-coordinate to place
         * @param y - y-coordinate to place
         * @return true if given coordinates are valid
         * @return false if given coordinates violate some condtions
         */
        public boolean insertGarry(int x, int y) {
            // Change coordinate systems to work with array indexing
            int buf = x; x = y; y = buf; x = invertX(x);
            if (x == (LIB_SIZE - 1) && y == 0) {
                return true;
            }
            return false;
        }


        public boolean insertFilch(int x, int y) {
            int buf = x; x = y; y = buf; x = invertX(x);
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            filch = insertZone(x, y, 2, map);
            map[filch.x][filch.y] = CellContent.FILCH;
            if (map[harry.x][harry.y] == CellContent._____) {
                return true;
            }
            return false;
        }

        
        public boolean insertNorris(int x, int y) {
            int buf = x; x = y; y = buf; x = invertX(x);
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            norris = insertZone(x, y, 1, map);
            map[norris.x][norris.y] = CellContent.NORRI;
            if (map[harry.x][harry.y] == CellContent._____ && norris.equals(filch) == false) {
                return true;
            }
            else {
                return false;
            }
        }


        public Tuple insertZone(int x, int y, int zoneSize, CellContent[][] map) {
            for (int i = 0; i < 1 + zoneSize * 2; ++i) {
                for (int j = 0; j < 1 + zoneSize * 2; ++j) {
                    int u = x + i - zoneSize;
                    int v = y + j - zoneSize;
                    if (isInsideTheLibrary(u, v)) {
                        map[u][v] = CellContent.ZONE_;
                    }
                }
            }

            return new Tuple(x, y);
        }

        
        public boolean insertBook(int x, int y) {
            int buf = x; x = y; y = buf; x = invertX(x);
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            book = new Tuple(x, y);
            if (map[x][y] == CellContent._____) {
                return true;
            }
            return false;
        }

        
        public boolean insertExit(int x, int y) {
            int buf = x; x = y; y = buf; x = invertX(x);
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            exit = new Tuple(x, y);
            if (map[x][y] != CellContent.FILCH &&
                map[x][y] != CellContent.NORRI &&
                exit.equals(book) == false) {
                    return true;
                }
            return false;
        }

        
        public boolean insertCloak(int x, int y) {
            int buf = x; x = y; y = buf; x = invertX(x);
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            cloak = new Tuple(x, y);
            if (map[x][y] == CellContent._____) {
                return true;
            }
            return false;
        }
        
        /**
         * Needed for coordinate system conversion (to go to the array indexing)
         * @param x - given x-coordinate
         * @return converted coordinate
         */
        int invertX(int x) {
            return LIB_SIZE - 1 - x;
        }
    }

    /**
     * Represents Harry - our actor in book finding simulation
     * Contains both alogorithms for task solving and supportive methods
     */
    static class Harry {
        /** Library object to work with */
        Library library;
        /** Map of the world */
        CellContent[][] map;

        /**
         * Harry is initialized by Library object
         * @param library - created Library object for algoritms to work with
         */
        public Harry(Library library) {
            this.library = library;
            map = library.getMap();
        }

        /** Keeps what percive mode Harry should use in current sumulation */
        int perceiveMode;

        /** 
         * Used for backtracking
         * Global for all recursion calls
         * Keeps 2D map with shortest path that has been found
        */
        CellContent[][] shortestPassMap;

        /** 
         * Used for backtracking
         * Global for all recursion calls (to avoid copiing time overhead)
         * Keeps path for current recursive call
        */
        Vector<Tuple> currentPath;

        /** 
         * Used for backtracking
         * Global for all recursion calls
         * Keeps list of cells with the shortest path
        */
        Vector<Tuple> shortestPath;

        /** 
         * Used for backtracking
         * Global for all recursion calls
         * Keeps coordinates of object, which Harry is looking at the moment
         * for example: book or exit
        */
        Tuple destination;

        /** 
         * Used for backtracking
         * Global for all recursion calls
         * Keeps whether the cloak was found during the shortest pass
        */
        boolean isCloakFound;

        /** 
         * Used for backtracking
         * Global for all recursion calls
         * Specifies whether the pass should contain finding of the cloak
        */
        boolean mustFindCloak;


        /**
         * This method computes the shortest solution for book finding
         * using backtracking method
         * @param perceiveMode Harry's vision variant (first or second)
         */
        public void launchBacktracking(int perceiveMode) {
            /** Used to meassure execution time */
            long startTime = System.nanoTime();

            try {

                // Set percieve mode
                this.perceiveMode = perceiveMode;

                System.out.println("Backtracking algorithm:\n");

                // At this part we will find the shortest path to the book
                currentPath = new Vector<Tuple>();
                shortestPath = new Vector<Tuple>();
                mustFindCloak = false;
                destination = library.book;
                backtracking(library.harry.x, library.harry.y, library.createEmptyMap(), false);
                Vector<Tuple> pathToBook = new Vector<Tuple>(shortestPath);
                CellContent[][] mapToBook = library.copyMap(shortestPassMap);

                // If pathToBook is empty it means that there is no path from Harry to book
                if (pathToBook.size() == 0) {
                    System.out.println("Defeat: Harry can't reach the book.");
                    System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
                    return;
                }

                currentPath = new Vector<Tuple>();
                shortestPath = new Vector<Tuple>();
                destination = library.exit;
                backtracking(pathToBook.lastElement().x, pathToBook.lastElement().y,
                            library.cleanFromVisitedMarkers(shortestPassMap), isCloakFound);
                Vector<Tuple> pathToExit = new Vector<Tuple>(shortestPath);
                CellContent[][] mapToExit = library.copyMap(shortestPassMap);

                if (pathToExit.size() == 0) {
                    System.out.println("Defeat: Harry can't reach the book.");
                    System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
                    return;
                }

                /*
                If the shortest pass to the book does not finds the cloak
                it is not always effective in exit finding
                So, here the shortest path to the book will include finding of the cloak
                */
                currentPath = new Vector<Tuple>();
                shortestPath = new Vector<Tuple>();
                mustFindCloak = true;
                destination = library.book;
                backtracking(LIB_SIZE - 1, 0, library.createEmptyMap(), false);
                Vector<Tuple> pathToBookWithCloak = new Vector<Tuple>(shortestPath);
                CellContent[][] mapToBookWithCloak = library.copyMap(shortestPassMap);

                // If there is no path to book using cloak, then the shortest path was found in upper call to backtracking
                if (pathToBookWithCloak.size() == 0) {
                    System.out.println("Victory!\n");
                    System.out.println("Path from start to the book:");
                    library.printMap(mapToBook);
                    printPath(pathToBook);
                    System.out.println("Path from book to exit:");
                    library.printMap(mapToExit);
                    printPath(pathToExit);
                    System.out.println("The total length: " + (pathToBook.size() + pathToExit.size() - 1));
                    System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
                    return;
                }

                currentPath = new Vector<Tuple>();
                shortestPath = new Vector<Tuple>();
                destination = library.exit;
                backtracking(pathToBookWithCloak.lastElement().x, pathToBookWithCloak.lastElement().y,
                                library.cleanFromVisitedMarkers(shortestPassMap), isCloakFound);
                Vector<Tuple> pathToExitWithCloak = new Vector<Tuple>(shortestPath);
                CellContent[][] mapToExitWithCloak = library.copyMap(shortestPassMap);

                // Here we will define what path was the shortest
                if (pathToExitWithCloak.size() == 0 ||
                    pathToBook.size() + pathToExit.size() < pathToBookWithCloak.size() + pathToExitWithCloak.size()) {
                    System.out.println("Victory!\n");
                    System.out.println("Path from start to the book:");
                    library.printMap(mapToBook);
                    printPath(pathToBook);
                    System.out.println("Path from book to exit:");
                    library.printMap(mapToExit);
                    printPath(pathToExit);
                    System.out.println("The total length: " + (pathToBook.size() + pathToExit.size() - 1));
                }
                else {
                    System.out.println("Victory!\n");
                    System.out.println("Path from start to the book:");
                    library.printMap(mapToBookWithCloak);
                    printPath(pathToBookWithCloak);
                    System.out.println("Path from book to exit:");
                    library.printMap(mapToExitWithCloak);
                    printPath(pathToExitWithCloak);
                    System.out.println("The total length: " + (pathToBookWithCloak.size() + pathToExitWithCloak.size() - 1));
                }
            }
            catch(Exception e) {
                System.out.println("Defeat: Harry is closed by inspectors");
            }

            System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
        }


        /**
         * Recursive backtracking method
         * @param x - x-coordinate
         * @param y - y-coordinate
         * @param stateMap keeps information collected by Harry during current path
         *                 it stores "VISIT" markers for visited cells
         *                 and "ZONE" markers of seen isnpector's zones
         * @param cloak It is true if Harry has found cloak and false otherwise
         */
        void backtracking(int x, int y, CellContent[][] stateMap, boolean cloak) {
            // Check wether Harry can step on current cell
            if (isValidPosition(x, y, stateMap, cloak, map) == false) {
                return;
            }

            // If the current path is longer than shortest found path then we do not need to explore current path
            if (shortestPath.size() != 0 && currentPath.size() == shortestPath.size()) {
                return;
            }

            // If Harry accidentally step on cloak he finds it
            if ((new Tuple(x, y)).equals(library.cloak)) {
                cloak = true;
            }

            // Deep copy of stateMap for current recursive call
            stateMap = library.copyMap(stateMap);

            // Mark current position as visited
            stateMap[x][y] = CellContent.VISIT;

            // Add current position to currentPath vector
            currentPath.add(new Tuple(x, y));

            // Apply Harry's vision and "memorize" viewed inspectors' zones
            percive(x, y, stateMap, map);
            
            /* 
            If Harry reached the destination point (book or exit) we can compare the
            current path with the shortest found path
            */
            if ((new Tuple(x, y)).equals(destination)) {
                if (shortestPath.size() == 0 || shortestPath.size() > currentPath.size()){
                    if (mustFindCloak == false || cloak == true) {
                        shortestPath.clear();
                        shortestPath.addAll(currentPath);
                        shortestPassMap = library.copyMap(stateMap);
                        isCloakFound = cloak;
                    }
                }
                currentPath.remove(currentPath.size() - 1);
                return;
            }

            // Run backtracking step for all neighboring cells
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    backtracking(x + i - 1, y + j - 1, stateMap, cloak);
                }
            }

            currentPath.remove(currentPath.size() - 1);
        }

        /**
         * This method computes the shortest solution for book finding
         * using BFS algoritm
         * 
         * First, it computes path start->book->exit
         * Second, start->cloak->book->exit
         * Third, start->book->cloak->exit
         * 
         * Finally, algorithm chooses the shortest path from these combinations
         * 
         * @param perceiveMode Harry's vision mode (first or second)
         */
        public void launchBFS(int perceiveMode) {
            /** Used to meassure algorithm working time */
            long startTime = System.nanoTime();

            this.perceiveMode = perceiveMode;

            /** Shortest path from start to the book */
            Vector<Tuple> shortestToBook = null;

            /** Shortest path from the book to exit */
            Vector<Tuple> shortestToExit = null;

            /** Length of the found shortest path */
            int shortestPath = (int) 1e9;

            System.out.println("BFS algorithm:\n");
            
            try {
                // At first we will check path without finding the cloak
                // start->book->exit
                Vector<Tuple> pathToBook = bfs(library.harry, library.book, false);
                Vector<Tuple> pathToExit = bfs(library.book, library.exit, false);

                // Save found path if it is exists
                if(pathToBook.size() != 0 && pathToExit.size() != 0) {
                    shortestToBook = pathToBook;
                    shortestToExit = pathToExit;
                    shortestPath = shortestToBook.size() + shortestToExit.size();
                }
            
            
                // Now we will check finding the cloak before the book
                // start->cloak->book->exit
                Vector<Tuple> startToCloak = bfs(library.harry, library.cloak, false);
                Vector<Tuple> cloakToBook = bfs(library.cloak, library.book, true);
                Vector<Tuple> bookToExit = bfs(library.book, library.exit, true);
                
                // Decide which of found paths is the shortest
                if(startToCloak.size() != 0 && cloakToBook.size() != 0  && bookToExit.size() != 0) {
                    int currPath = startToCloak.size() + cloakToBook.size() + bookToExit.size();

                    if (currPath < shortestPath) {
                        startToCloak.remove(startToCloak.size() - 1);
                        startToCloak.addAll(cloakToBook);

                        shortestToBook = startToCloak;
                        shortestToExit = bookToExit;
                        shortestPath = currPath;
                    }
                }
        

                // Finally we need to check finding the cloak after the book
                // start->book->cloak->exit
                Vector<Tuple> startToBook = bfs(library.harry, library.book, false);
                Vector<Tuple> bookToCloak = bfs(library.book, library.cloak, false);
                Vector<Tuple> cloakToExit = bfs(library.cloak, library.exit, true);
            
                if (startToBook.size() != 0 && bookToCloak.size() != 0  && cloakToExit.size() != 0) {
                    int currPath = startToBook.size() + bookToCloak.size() + cloakToExit.size();

                    if (currPath < shortestPath) {
                        bookToCloak.remove(bookToCloak.size() - 1);
                        bookToCloak.addAll(cloakToExit);

                        shortestToBook = startToBook;
                        shortestToExit = bookToCloak;
                        shortestPath = currPath;
                    }
                }
            }
            // If Harry will be caught "try" expression will throw exception
            catch(Exception e) {
                System.out.println("Defeat: Harry was caught");
                System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
                return;
            }


            if (shortestPath == (int) 1e9) {
                System.out.println("Defeat: Harry can't reach the book.");
            }
            else {
                System.out.println("Path from start to the book:");
                printPath(shortestToBook);
                System.out.println("Path from book to exit:");
                printPath(shortestToExit);
                System.out.println("The total length: " + (shortestPath - 1));
            }
            System.out.println("Working time: " + (System.nanoTime() - startTime) / 1000000 + " ms");
        }


        /**
         * Breadth-first search algorithm for finding shortest paths
         * @param start coordinates of start point
         * @param finish coordinates of the point of interest
         * @param cloak true if Harry has cloak on him and false otherwise
         * @return found shrotest path from start to finish
         */
        Vector<Tuple> bfs(Tuple start, Tuple finish, boolean cloak) {
            /**
             * Represents Harry's memory:
             * stores seen inspector's zones
             * stores visited and queued markers
             */
            CellContent[][] stateMap = library.createEmptyMap();

            /** Stores for each cell it's ancestor for path restoring */
            Tuple[][] savedPath = new Tuple[LIB_SIZE][LIB_SIZE];

            /** queue for usual bfs implementation */
            Queue<Tuple> queue = new LinkedList<Tuple>();
            queue.add(start);

            while (queue.size() != 0) {
                Tuple curr = queue.remove();
                stateMap[curr.x][curr.y] = CellContent.VISIT;

                // Apply Harry's vision to store information seen
                percive(curr.x, curr.y, stateMap, map);

                // Process all adjacent cells 
                for (int i = 0; i < 3; ++i) {
                    for (int j = 0; j < 3; ++j) {
                        int x = curr.x + i - 1;
                        int y = curr.y + j - 1;

                        // Check whether considered cell is valid in sence of Harry
                        if (isInsideTheLibrary(x, y) &&
                            (stateMap[x][y] == CellContent._____ || 
                            (stateMap[x][y] == CellContent.ZONE_ && cloak == true))) {
                            
                            stateMap[x][y] = CellContent.QUEUE;
                            savedPath[x][y] = new Tuple(curr.x, curr.y);
                            queue.add(new Tuple(x, y));
                            
                            // Return null if Harry has failed the task and occasionaly
                            // was caught by inspectors
                            if ((map[x][y] == CellContent.FILCH ||
                                map[x][y] == CellContent.NORRI ||
                                map[x][y] == CellContent.ZONE_) && cloak == false) {
                                return null;
                            }
                        }
                    }
                }
            } 

            // Restore found shortest path
            Vector<Tuple> path = new Vector<Tuple>();
            Tuple tmp = savedPath[finish.x][finish.y];
            while (tmp != null) {
                path.add(tmp);
                tmp = savedPath[tmp.x][tmp.y];
            }
            Collections.reverse(path);
            if (path.size() != 0) {
                path.add(finish);
            }

            return path;
        }


        /**
         * Launches one of the vision algoritms to use Harry's vision and
         * save observed results into stateMap
         * @param x x-coordinate of Harry
         * @param y y-coordinate of Harry
         * @param stateMap this matrix represents Harry's memory
         * @param map real world map
         */
        void percive(int x, int y, CellContent[][] stateMap, CellContent[][] map) {
            if (perceiveMode == 1) {
                firstVariant(x, y, stateMap, map);
            }
            else {
                secondVariant(x, y, stateMap, map);
            }
        }


        /**
         * Searches cells around Harry according to vison scheme 1
         * @param x x-coordinate of Harry
         * @param y y-coordinate of Harry
         * @param stateMap this matrix represents Harry's memory
         * @param map real world map
         */
        void firstVariant(int x, int y, CellContent[][] stateMap, CellContent[][] map) {
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 3; ++j) {
                    int u = x + i - 1;
                    int v = y + j - 1;
                    applyVision(u, v, stateMap, map);
                }
            }
        }


        /**
         * Searches cells around Harry according to vison scheme 2
         * @param x x-coordinate of Harry
         * @param y y-coordinate of Harry
         * @param stateMap this matrix represents Harry's memory
         * @param map real world map
         */
        void secondVariant(int x, int y, CellContent[][] stateMap, CellContent[][] map) {
            for (int i = 0; i < 3; ++i) {
                applyVision(x + i - 1, y + 2, stateMap, map);
                applyVision(x + i - 1, y - 2, stateMap, map);
                applyVision(x + 2, y + i - 1, stateMap, map);
                applyVision(x - 2, y + i - 1, stateMap, map);
            }
        }


        /**
         * Analyzes given cell for the maintenance of inspectors and their zones
         * @param x x-coordinate of given cell
         * @param y y-coordinate of given cell
         * @param stateMap this matrix represents Harry's memory
         * @param map real world map
         */
        void applyVision(int x, int y, CellContent[][] stateMap, CellContent[][] map) {
            if (isInsideTheLibrary(x, y) && stateMap[x][y] != CellContent.VISIT) {
                if (map[x][y] == CellContent.ZONE_ ||
                    map[x][y] == CellContent.FILCH ||
                    map[x][y] == CellContent.NORRI) {
                    stateMap[x][y] = map[x][y];
                }
            }
        }
        

        /**
         * Used for backtracking
         * Checks whether Harry can step into the cell
         * @param x coordinate of cell
         * @param y coordinate of cell
         * @param stateMap this matrix represents Harry's memory
         * @param cloak true if Harry has cloak and false otherwise
         * @param map real world map
         * @return true if position is valid to step in sense of Harry, false otherwise
         */
        Boolean isValidPosition(int x, int y, CellContent[][] stateMap, boolean cloak, CellContent[][] map) {
            if (isInsideTheLibrary(x, y) == false) {
                return false;
            }
            if (stateMap[x][y] == CellContent.VISIT) {
                return false;
            }
            if (isDangerousCell(stateMap[x][y], cloak)) {
                return false;
            }
            return true;
        }
    
        
        /**
         * Check wether the cell is dangerous to step in
         * @param content cell's content in sence of Harry's memory
         * @param cloak true if Harry has cloak, false otherwise
         * @return true if cell is dangerous, false otherwise
         */
        Boolean isDangerousCell(CellContent content, boolean cloak) {
            if (content == CellContent.ZONE_ && cloak == false) {
                return true;
            }
            if (content == CellContent.FILCH || content == CellContent.NORRI) {
                return true;
            }
            return false;
        }
    }


    public static Boolean isInsideTheLibrary(int x, int y) {
        return (0 <= x && x < LIB_SIZE) && (0 <= y && y < LIB_SIZE);
    }


    public static void printPath(Vector<Tuple> path) {
        for (Tuple cell : path) {
            System.out.print("[" + cell.x + ", " + cell.y + "] ");
        }
        System.out.println("\n");
    }


    public static void printVector(Vector<Integer> vec) {
        for (Integer x : vec) {
            System.out.print(x + " ");
        }
        System.out.println();
    }

}


class Tuple {
    public final int x;
    public final int y;
    public Tuple(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public boolean equals(Tuple tuple) {
        if (tuple.x == this.x && tuple.y == this.y) {
            return true;
        }
        return false;
    }
}