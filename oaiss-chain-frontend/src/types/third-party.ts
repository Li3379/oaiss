/** 第三方机构实体 */
export interface ThirdPartyOrg {
  id: number
  userId: number
  orgName: string
  orgCode: string
  orgType: number
  supervisionScope: string
  contactPerson: string
  contactPhone: string
  address: string
  accessLevel: number
  status: number
  createdAt: string
  updatedAt: string
  deleted: boolean
}
