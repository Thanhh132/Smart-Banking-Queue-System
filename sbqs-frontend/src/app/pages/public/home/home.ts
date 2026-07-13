import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './home.html',
  styles: [`.public-message{display:grid;place-items:center;align-content:center;gap:10px;min-height:100vh;text-align:center}.public-message h1,.public-message p{margin:0}.public-message a{margin-top:10px;padding:10px 16px;border-radius:10px;background:#087d74;color:#fff;text-decoration:none;font-weight:800}`],
})
export class Home {}
