
---

# ✅ **JAVA COLLECTIONS CHEATSHEET **

**(Init → Add → Get → Remove → Notes)**

---

## **1. Array**

```java
int[] arr = new int[5];        // init fixed size
int[] arr2 = {1, 2, 3};        // init with values
arr[0] = 10;                   // set
int x = arr[0];                // get
```

✔ Fast access, fixed size, not resizable.

---

## **2. ArrayList**

```java
List<Integer> list = new ArrayList<>();
list.add(10);                  
list.add(1, 20);               // add at index
int x = list.get(0);           
list.set(0, 30);               
list.remove(1);                
```

✔ Dynamic array, fast random access, slower inserts in middle.
✔ Most commonly used in interviews.

---

## **3. LinkedList**

```java
List<Integer> ll = new LinkedList<>();
ll.add(10);
ll.addFirst(5);
ll.addLast(20);
int x = ll.get(0);
ll.removeFirst();
```

✔ Fast insert/delete at ends, slow random access.
✔ Used in queue/deque logic.

---

## **4. Vector**

```java
Vector<Integer> v = new Vector<>();
v.add(10);
v.get(0);
```

✔ Like ArrayList but **synchronized** (rarely used today).

---

## **5. Stack**

```java
Stack<Integer> st = new Stack<>();
st.push(10);
st.push(20);
int x = st.pop();
int top = st.peek();
```

✔ LIFO — used in parentheses, DFS, backtracking.

---

## **6. Queue (LinkedList Implementation)**

```java
Queue<Integer> q = new LinkedList<>();
q.offer(10);
q.offer(20);
int x = q.peek();
int y = q.poll();
```

✔ FIFO — used in BFS.

---

## **7. Deque**

```java
Deque<Integer> dq = new ArrayDeque<>();
dq.addFirst(10);
dq.addLast(20);
dq.removeFirst();
```

✔ Used for sliding window problems.

---

## **8. PriorityQueue (Min-Heap default)**

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(10);
pq.offer(5);
int x = pq.poll();   // returns smallest
```

▶ Max-heap:

```java
PriorityQueue<Integer> maxpq = new PriorityQueue<>(Collections.reverseOrder());
```

✔ Used in heaps, top-K, scheduling.

---

## **9. HashMap**

```java
Map<String, Integer> map = new HashMap<>();
map.put("a", 1);
map.put("b", 2);
int x = map.get("a");          // returns null if not present
boolean k = map.containsKey("a");
map.remove("b");
```

✔ Most used map; O(1) average ops.

---

## **10. TreeMap (Sorted Map)**

```java
TreeMap<Integer, String> tm = new TreeMap<>();
tm.put(10, "A");
tm.put(5, "B");
int firstKey = tm.firstKey();
```

✔ Keys always sorted (Red-Black tree).
✔ Used when order matters.

---

## **11. HashSet**

```java
Set<Integer> set = new HashSet<>();
set.add(10);
set.contains(10);
set.remove(10);
```

✔ Unique elements, O(1) ops, used in duplicates-related questions.

---

## **12. TreeSet (Sorted Set)**

```java
Set<Integer> ts = new TreeSet<>();
ts.add(10);
ts.add(5);
int x = ts.first();     // smallest
```

✔ Sorted unique elements.

---

## **13. StringBuilder**

```java
StringBuilder sb = new StringBuilder();
sb.append("A");
sb.append(10);
sb.toString();
```

✔ Used in string manipulation (faster than String).

---
