#lang racket

(require racket/base)
(require file/gunzip)

(define (file-exists-raise? f)
  (when (not (file-exists? f))
    (raise (string-append "file does not exist: " f)) #t))


(struct system-event (timestamp event-code))

;fixed size pipe ensures that the reader won't try to read the whole file in one go
(define-values (pipe-from pipe-to) (make-pipe (* 1024 1024)))
(with-input-from-file (vector-ref (current-command-line-arguments) 0)
   (λ ()
     ; needed in a separate thread because gunzip-through-ports blocks when the pipe's buffer is full
     (thread
      (lambda ()
        (gunzip-through-ports (current-input-port) pipe-to)
        (close-output-port pipe-to)))
     ; pulling data from the output end of the pipe will allow the thread to put more data onto it
     (define (loop)
       (let* ((sep (read-byte pipe-from))
              (len (read-byte pipe-from))
              (rec (read-bytes len pipe-from)))
         (printf "~a" (bytes->string/utf-8 (bytes (bytes-ref rec 0)))))
       (loop))
     (loop)))
