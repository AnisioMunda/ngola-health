import { Component } from '@angular/core';

@Component({
  selector: 'app-home',
  standalone: true,
  template: `
    <div style="
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100vh;
      font-family: Inter, sans-serif;
      background: #f5f6fa;
    ">
      <div style="
        background: white;
        border-radius: 12px;
        padding: 48px;
        text-align: center;
        box-shadow: 0 2px 8px rgba(0,0,0,0.08);
        max-width: 480px;
      ">
        <div style="
          width: 64px; height: 64px;
          background: #3949ab;
          border-radius: 16px;
          display: flex; align-items: center; justify-content: center;
          margin: 0 auto 24px;
          font-size: 32px; color: white;
        ">+</div>

        <h1 style="margin: 0 0 8px; font-size: 24px; font-weight: 600; color: #1a1a2e;">
          HospitalAO
        </h1>

        <p style="margin: 0 0 24px; color: #666; font-size: 15px;">
          Sistema de Gestão Hospitalar
        </p>

        <div style="
          background: #e8f5e9;
          border-radius: 8px;
          padding: 16px;
          margin-bottom: 24px;
        ">
          <p style="margin: 0; color: #2e7d32; font-size: 14px; font-weight: 500;">
            Sprint 0 concluído com sucesso
          </p>
          <p style="margin: 4px 0 0; color: #388e3c; font-size: 13px;">
            Infraestrutura Docker a funcionar
          </p>
        </div>

        <div style="text-align: left; font-size: 13px; color: #666;">
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
            <span style="color: #4caf50;">✓</span>
            <span>PostgreSQL ligado</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
            <span style="color: #4caf50;">✓</span>
            <span>Redis ligado</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
            <span style="color: #4caf50;">✓</span>
            <span>RabbitMQ ligado</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
            <span style="color: #4caf50;">✓</span>
            <span>MinIO ligado</span>
          </div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="color: #4caf50;">✓</span>
            <span>Spring Boot a correr</span>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class HomeComponent {}