import * as echarts from 'echarts/core'

import { BarChart } from 'echarts/charts'
import { LineChart } from 'echarts/charts'
import { PieChart } from 'echarts/charts'

import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DatasetComponent,
  TitleComponent,
} from 'echarts/components'

import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DatasetComponent,
  TitleComponent,
  CanvasRenderer,
])

export default echarts
