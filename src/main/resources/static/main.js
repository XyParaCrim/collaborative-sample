$(function () {
  window.onload = function () {
    let editor = monaco.editor.create(document.getElementById('container'), {
      value: [
        'function x() {',
        '\tconsole.log("Hello world!");',
        '}'
      ].join('\n'),
      language: 'javascript'
    });

    editor.onDidChangeModelContent(console.log)

    let socket = io("http://localhost:8081")

    window.socket = socket
    window.editor = editor
  }
})