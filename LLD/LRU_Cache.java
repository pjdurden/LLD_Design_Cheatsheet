// import java.util.HashMap;
// import java.util.Map;

// public class LRU_Cache {
//     // Least Recently Used cache, which is a caching algorithm that removes the least recently accessed items when the cache is full. 
//     // Operations - put , get 
//     // fixed capacity
//     // thread safe , concurrent access multiple threads 
//     // O(1) put and get operations 
//     // 

//     // map to save occurences
//     // a queue -> push back , pop front
//     // 

//     // classes , 

//     private class Node<K,V>{
//         K key;
//         V value;
//         Node<K,V> prev;
//         Node<K,V> next;
//         public Node(K key , V value)
//         {
//             this.key = key;
//             this.value = value;
//         }
//     }

//     private static class LRUCache<K,V>{
//         private final int capacity;
//         private final Map<K, Node<K, V>> cache;
//         private final Node<K, V> head;
//         private final Node<K, V> tail;

//         public LRUCache(int capacity) {
//             this.capacity = capacity;
//             cache = new HashMap<>(capacity);
//             head = new Node<>(null, null);
//             tail = new Node<>(null, null);
//             head.next = tail;
//             tail.prev = head;
//         }

        
//     }




//     }
// }
